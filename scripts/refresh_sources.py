#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""同步内置直播源。

数据来源：
  - 央视/卫视（IPv4）: fanmingming/live  tv/m3u/index.m3u
  - 央视/卫视（IPv6）: fanmingming/live  tv/m3u/ipv6.m3u
  - 港澳台及国际   : iptv-org/iptv  countries/{cn,hk,mo,tw}.m3u

产物：
  sources/dist/*.m3u                     （进 git，供在线订阅 / 直接下载）
  core/data/src/main/assets/sources/*.m3u（打包进 APK，随 App 内置）

用法：
  python3 scripts/refresh_sources.py          # 抓取并写入（失败会保留旧文件）
  python3 scripts/refresh_sources.py --check  # 只校验现有产物是否健康
"""

from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import re
import sys
import time
import urllib.error
import urllib.request

from build_pack import probe_all
from hkmotw_ids import HKMOTW

ROOT = pathlib.Path(__file__).resolve().parents[1]
DIST = ROOT / "sources" / "dist"
ASSETS = ROOT / "core" / "data" / "src" / "main" / "assets" / "sources"

FANMINGMING_RAW = "https://raw.githubusercontent.com/fanmingming/live/main"
FANMINGMING = FANMINGMING_RAW + "/tv/m3u/"
IPTV_ORG = "https://iptv-org.github.io/iptv/countries/"
IPTV_ORG_EPG = "https://iptv-org.github.io/epg/guides/cn.xml"

UA = "Mozilla/5.0 (compatible; tv-live-source-bot/1.0)"
MIN_CHANNELS = 50
MAX_INTL_LINES = 3
MAX_INTL_FALLBACK = 2
RETRY = 2


def fetch(url: str) -> str:
    last: Exception | None = None
    for attempt in range(1, RETRY + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=25) as resp:
                return resp.read().decode("utf-8", errors="replace")
        except Exception as exc:  # noqa: BLE001
            last = exc
            if attempt < RETRY:
                time.sleep(attempt * 3)
    raise RuntimeError(f"拉取失败 {url}: {last}")


def parse_playlist(text: str) -> tuple[str, list[str]]:
    """把 m3u 拆成 (头部 #EXTM3U 行, [单条频道块])。"""
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    if not lines or not lines[0].startswith("#EXTM3U"):
        raise ValueError("不是合法的 m3u（缺少 #EXTM3U）")

    header, blocks, current = lines[0], [], []
    for line in lines[1:]:
        if line.startswith("#EXTINF"):
            if current:
                blocks.append(current)
            current = [line]
        elif line.startswith("#"):
            if current:
                current.append(line)
        else:  # 流地址
            if current:
                current.append(line)
                blocks.append(current)
                current = []
    if current and len(current) > 1:
        blocks.append(current)

    blocks = [b for b in blocks if b[-1] and not b[-1].startswith("#")]
    return header, ["\n".join(b) for b in blocks]


def dedupe(blocks: list[str]) -> list[str]:
    seen, out = set(), []
    for block in blocks:
        url = block.splitlines()[-1]
        key = re.sub(r"^https?://", "", url)
        if key in seen:
            continue
        seen.add(key)
        out.append(block)
    return out


def _block_key(block: str) -> str:
    first = block.splitlines()[0]
    match = re.search(r'tvg-id="([^"]+)"', first)
    if match:
        return match.group(1).split("@", 1)[0]
    return first.split(",")[-1].strip()


def probe_blocks(blocks: list[str]) -> list[str]:
    """对国际源逐条探测；每台优先留活线，没活线才留少量兜底。"""
    items = [{"url": b.splitlines()[-1]} for b in blocks]
    items = probe_all(items)
    rank = {True: 0, None: 1, False: 2}

    grouped: dict[str, list[tuple[str, dict]]] = {}
    for block, item in zip(blocks, items):
        grouped.setdefault(_block_key(block), []).append((block, item))

    selected: list[str] = []
    for pairs in grouped.values():
        ordered = sorted(
            pairs,
            key=lambda p: (rank.get(p[1].get("alive"), 2), p[1].get("cost", 99)),
        )
        alive = [p for p in ordered if p[1].get("alive") is True]
        fallback = [p for p in ordered if p[1].get("alive") is not True]
        chosen = alive[:MAX_INTL_LINES] if alive else fallback[:MAX_INTL_FALLBACK]
        selected.extend(block for block, _ in chosen)

    alive = sum(1 for i in items if i.get("alive") is True)
    print(f"  国际源探测：可用 {alive}/{len(items)}，保留 {len(selected)}/{len(blocks)} 条")
    payload = {
        "generated_at": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "summary": {
            "total": len(items),
            "alive": alive,
            "unknown": sum(1 for i in items if i.get("alive") is None),
            "dead": sum(1 for i in items if i.get("alive") is False),
            "selected": len(selected),
        },
        "lines": [
            {
                "url": i.get("url", ""),
                "alive": i.get("alive"),
                "kind": i.get("kind", ""),
                "status_code": i.get("status_code"),
                "latency_ms": round(1000 * float(i.get("cost", 0))),
            }
            for i in items
        ],
    }
    (DIST / "intl-line-stats.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return selected


def write(path: pathlib.Path, header: str, blocks: list[str], title: str, probe: bool = False) -> bool:
    if probe:
        blocks = probe_blocks(blocks)
    if len(blocks) < MIN_CHANNELS:
        print(f"  [跳过] {title}: 只有 {len(blocks)} 个频道，低于阈值 {MIN_CHANNELS}，保留旧文件")
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text("\n".join([header] + blocks) + "\n", encoding="utf-8")
    tmp.replace(path)
    print(f"  {title:24s} -> {path.relative_to(ROOT)}  {len(blocks)} 个频道")
    return True


def fix_dead_hosts(text: str) -> str:
    """把已失效的 live.fanmingming.com/.cn 换成 GitHub 直链。"""
    return text.replace("https://live.fanmingming.com", FANMINGMING_RAW) \
               .replace("https://live.fanmingming.cn", FANMINGMING_RAW)


def build_fanmingming(source_file: str, out_name: str, title: str) -> bool:
    header, blocks = parse_playlist(fetch(FANMINGMING + source_file))
    # 源站 .com / .cn 均已失效，统一改 GitHub 直链
    header = fix_dead_hosts(header)
    blocks = [fix_dead_hosts(b) for b in blocks]
    return write(DIST / out_name, header, dedupe(blocks), title)


def build_intl() -> bool:
    header = f'#EXTM3U x-tvg-url="{IPTV_ORG_EPG}"'
    blocks: list[str] = []
    failed: list[str] = []
    for code in ("cn", "hk", "mo", "tw"):
        try:
            _, part = parse_playlist(fetch(IPTV_ORG + f"{code}.m3u"))
        except Exception as exc:  # noqa: BLE001
            print(f"  [警告] iptv-org {code} 拉取失败：{exc}")
            failed.append(code)
            continue
        # 港澳台里并入总包的那批，不再在 intl 里重复出现
        kept = []
        for block in part:
            m = re.search(r'tvg-id="([^"]*?)@', block)
            if m and m.group(1) in HKMOTW:
                continue
            kept.append(block)
        print(f"  iptv-org {code}: {len(part)} 个频道，保留 {len(kept)} 个")
        blocks.extend(kept)

    if failed:
        print(f"  [警告] 缺少 {', '.join(failed)}，本轮不覆盖国际源")
        return False
    return write(DIST / "intl-hkmo-tw.m3u", header, dedupe(blocks), "港澳台及国际", probe=True)


def copy_to_assets() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    for src in DIST.glob("*.m3u"):
        (ASSETS / src.name).write_bytes(src.read_bytes())


def check() -> int:
    failed = 0
    files = sorted(DIST.glob("*.m3u"))
    if not files:
        print("sources/dist 下没有 m3u", file=sys.stderr)
        return 1
    for f in files:
        try:
            _, blocks = parse_playlist(f.read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001
            print(f"[FAIL] {f.name}: {exc}")
            failed += 1
            continue
        ok = len(blocks) >= MIN_CHANNELS
        print(f"[{'OK' if ok else 'FAIL'}] {f.name}: {len(blocks)} 个频道")
        failed += 0 if ok else 1
    return 1 if failed else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="只校验现有产物")
    args = parser.parse_args()

    if args.check:
        return check()

    print("同步直播源…")
    # 央视/卫视源由 scripts/build_pack.py 负责（多线路合并 + 探测）
    results = [build_intl()]
    copy_to_assets()

    if not all(results):
        # 单个上游超时不阻塞整条流水线：旧文件还在，build_pack 会继续用旧 intl 包，
        # 后面的健康检查会再判断结构是否完整。
        print("部分源未更新（已保留旧文件），继续后续合并。")
    print("完成，已同步到 assets。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
