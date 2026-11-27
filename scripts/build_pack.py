#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把多个上游源合并成「同频道多线路」的直播源包。

思路：
  1. 拉多个候选源（fanmingming 的 index/itv/ipv6 + iptv-org 中国区）
  2. 按标准频道名归一（CCTV-1 / CCTV1 / 央视一套 → CCTV1；Hunan TV → 湖南卫视）
  3. 并发探测每条流：能播的排前面，播不了的排后面（不丢弃，交给 App 自动换线）
  4. 同一频道最多保留 MAX_LINES 条线路
  5. 输出 sources/dist/pack-cctv-satellite.m3u 并同步到 assets

用法：
  python3 scripts/build_pack.py             # 拉取 + 探测 + 生成
  python3 scripts/build_pack.py --no-probe  # 不探测，仅合并排序
"""

from __future__ import annotations

import argparse
import concurrent.futures as futures
import pathlib
import re
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
DIST = ROOT / "sources" / "dist"
ASSETS = ROOT / "core" / "data" / "src" / "main" / "assets" / "sources"

FM_RAW = "https://raw.githubusercontent.com/fanmingming/live/main"
FM = FM_RAW + "/tv/m3u/"
IPTV_ORG = "https://iptv-org.github.io/iptv/countries/"

UA = "okhttp/4.12.0"
MAX_LINES = 6
PROBE_TIMEOUT = 8
PROBE_WORKERS = 32

CCTV_ALIAS = {
    "cctv1": "CCTV1", "cctv1综合": "CCTV1", "央视一套": "CCTV1", "cctv1综合频道": "CCTV1",
    "cctv2": "CCTV2", "央视二套": "CCTV2", "cctv2财经": "CCTV2",
    "cctv3": "CCTV3", "央视三套": "CCTV3", "cctv3综艺": "CCTV3",
    "cctv4": "CCTV4", "央视四套": "CCTV4", "cctv4中文国际": "CCTV4",
    "cctv5": "CCTV5", "央视五套": "CCTV5", "cctv5体育": "CCTV5",
    "cctv5+": "CCTV5+", "cctv5plus": "CCTV5+", "cctv5+体育赛事": "CCTV5+",
    "cctv6": "CCTV6", "央视六套": "CCTV6", "cctv6电影": "CCTV6",
    "cctv7": "CCTV7", "cctv7国防军事": "CCTV7",
    "cctv8": "CCTV8", "cctv8电视剧": "CCTV8",
    "cctv9": "CCTV9", "cctv9纪录": "CCTV9",
    "cctv10": "CCTV10", "cctv10科教": "CCTV10",
    "cctv11": "CCTV11", "cctv11戏曲": "CCTV11",
    "cctv12": "CCTV12", "cctv12社会与法": "CCTV12",
    "cctv13": "CCTV13", "cctv13新闻": "CCTV13",
    "cctv14": "CCTV14", "cctv14少儿": "CCTV14",
    "cctv15": "CCTV15", "cctv15音乐": "CCTV15",
    "cctv16": "CCTV16", "cctv16奥林匹克": "CCTV16",
    "cctv17": "CCTV17", "cctv17农业农村": "CCTV17",
}

SATELLITE = {
    "湖南卫视": ["湖南卫视", "hunan tv", "hunan satellite"],
    "浙江卫视": ["浙江卫视", "zhejiang tv", "zhejiang satellite"],
    "江苏卫视": ["江苏卫视", "jiangsu tv", "jiangsu satellite"],
    "东方卫视": ["东方卫视", "dragon tv"],
    "北京卫视": ["北京卫视", "beijing tv", "beijing satellite"],
    "深圳卫视": ["深圳卫视", "shenzhen tv"],
    "广东卫视": ["广东卫视", "guangdong tv"],
    "安徽卫视": ["安徽卫视", "anhui tv"],
    "山东卫视": ["山东卫视", "shandong tv"],
    "湖北卫视": ["湖北卫视", "hubei tv"],
    "天津卫视": ["天津卫视", "tianjin tv"],
    "四川卫视": ["四川卫视", "sichuan tv"],
    "重庆卫视": ["重庆卫视", "chongqing tv"],
    "河南卫视": ["河南卫视", "henan tv"],
    "河北卫视": ["河北卫视", "hebei tv"],
    "辽宁卫视": ["辽宁卫视", "liaoning tv"],
    "黑龙江卫视": ["黑龙江卫视", "heilongjiang tv"],
    "吉林卫视": ["吉林卫视", "jilin tv"],
    "江西卫视": ["江西卫视", "jiangxi tv"],
    "东南卫视": ["东南卫视", "southeast tv"],
    "贵州卫视": ["贵州卫视", "guizhou tv"],
    "云南卫视": ["云南卫视", "yunnan tv"],
    "广西卫视": ["广西卫视", "guangxi tv"],
    "山西卫视": ["山西卫视", "shanxi tv"],
    "陕西卫视": ["陕西卫视", "shaanxi tv"],
    "甘肃卫视": ["甘肃卫视", "gansu tv"],
    "宁夏卫视": ["宁夏卫视", "ningxia tv"],
    "青海卫视": ["青海卫视", "qinghai tv"],
    "新疆卫视": ["新疆卫视", "xinjiang tv"],
    "西藏卫视": ["西藏卫视", "tibet tv"],
    "内蒙古卫视": ["内蒙古卫视", "inner mongolia tv"],
    "海南卫视": ["海南卫视", "hainan tv"],
    "三沙卫视": ["三沙卫视", "sansha tv"],
    "厦门卫视": ["厦门卫视", "xiamen tv"],
    "延边卫视": ["延边卫视", "yanbian tv"],
    "康巴卫视": ["康巴卫视", "kangba tv"],
    "安多卫视": ["安多卫视", "ando tv"],
    "CETV1": ["cetv1", "中国教育一台"],
    "CETV2": ["cetv2", "中国教育二台"],
    "CETV4": ["cetv4", "中国教育四台"],
}

from hkmotw_ids import HKMOTW, HKMOTW_GROUP, HKMOTW_ORDER, HKMOTW_BACKUP


# 卫视显示顺序：先一线卫视，再按大区，最后教育台
SATELLITE_ORDER = [
    "湖南卫视", "浙江卫视", "江苏卫视", "东方卫视", "北京卫视", "深圳卫视", "广东卫视",
    "安徽卫视", "山东卫视", "湖北卫视", "天津卫视", "四川卫视", "重庆卫视", "河南卫视",
    "河北卫视", "辽宁卫视", "黑龙江卫视", "吉林卫视", "江西卫视", "东南卫视", "贵州卫视",
    "云南卫视", "广西卫视", "山西卫视", "陕西卫视", "甘肃卫视", "宁夏卫视", "青海卫视",
    "新疆卫视", "西藏卫视", "内蒙古卫视", "海南卫视", "三沙卫视", "厦门卫视", "延边卫视",
    "康巴卫视", "安多卫视", "CETV1", "CETV2", "CETV4",
]


def channel_sort_key(std: str) -> tuple:
    """频道排序：央视按数字，卫视按习惯顺序，港澳台按地区顺序。"""
    if std.startswith("CCTV"):
        m = re.match(r"^CCTV(\d+)([+K]?)$", std)
        if m:
            tag_rank = {"": 0, "+": 1, "K": 2}.get(m.group(2), 3)
            return (0, int(m.group(1)), tag_rank, std)
        return (0, 99, 9, std)
    if std.startswith("CETV"):
        m = re.match(r"^CETV(\d+)$", std)
        return (1, int(m.group(1)) if m else 99, 0, std)
    if std in SATELLITE_ORDER:
        return (2, SATELLITE_ORDER.index(std), 0, std)
    if std in HKMOTW_ORDER:
        return (3, HKMOTW_ORDER.index(std), 0, std)
    return (4, 0, 0, std)


def standard_name(raw: str) -> str | None:
    """把别名收敛成标准频道名；认不出来返回 None。"""
    name = re.sub(r"[\(（\[【].*?[\)）\]】]", "", raw).strip()
    key = re.sub(r"[\s\-_·]+", "", name).lower()
    key = key.replace("高清", "").replace("超清", "").replace("标清", "")

    m = re.match(r"^cctv(\d{1,2})(\+)?$", key)
    if m:
        return f"CCTV{m.group(1)}{'+' if m.group(2) else ''}"
    if key in CCTV_ALIAS:
        return CCTV_ALIAS[key]

    low = name.lower()
    for std, aliases in SATELLITE.items():
        for alias in aliases:
            if alias in low:
                return std
    return None


def fetch(url: str, timeout: int = 60) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read().decode("utf-8", errors="replace")


def parse(text: str) -> list[dict]:
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    if not lines or not lines[0].startswith("#EXTM3U"):
        return []
    items, cur = [], None
    for line in lines[1:]:
        if line.startswith("#EXTINF"):
            name = line.split(",")[-1].strip()
            cur = {
                "raw_name": name,
                "tvg_id": (re.search(r'tvg-id="(.*?)"', line) or [None, ""])[1].split("@")[0],
                "tvg_name": (re.search(r'tvg-name="(.*?)"', line) or [None, ""])[1] or name,
                "logo": (re.search(r'tvg-logo="(.*?)"', line) or [None, ""])[1],
                "group": (re.search(r'group-title="(.*?)"', line) or [None, ""])[1],
            }
        elif line.startswith("#"):
            continue
        elif cur is not None:
            cur["url"] = line
            items.append(cur)
            cur = None
    return items


CANDIDATES = [
    ("fanmingming index", FM + "index.m3u"),
    ("fanmingming itv", FM + "itv.m3u"),
    ("fanmingming ipv6", FM + "ipv6.m3u"),
    ("iptv-org cn", IPTV_ORG + "cn.m3u"),
]


def collect() -> list[dict]:
    pool: dict[str, dict] = {}
    for label, url in CANDIDATES:
        try:
            items = parse(fetch(url))
        except Exception as exc:  # noqa: BLE001
            print(f"  [警告] {label} 拉取失败：{exc}")
            continue
        kept = 0
        for it in items:
            std = standard_name(it["raw_name"]) or standard_name(it["tvg_name"])
            if not std or not it["url"] or it["url"].startswith("rtp"):
                continue
            it["std"] = std
            if it["url"] in pool:
                continue
            pool[it["url"]] = it
            kept += 1
        print(f"  {label}: {len(items)} 条，命中 {kept} 条")
    return list(pool.values())


def probe(item: dict) -> dict:
    start = time.time()
    try:
        req = urllib.request.Request(item["url"], headers={"User-Agent": UA, "Range": "bytes=0-2048"})
        with urllib.request.urlopen(req, timeout=PROBE_TIMEOUT) as resp:
            body = resp.read(2048)
            item["alive"] = resp.status in (200, 206) and (b"#EXTM3U" in body or b"#EXT-X-" in body)
    except Exception:  # noqa: BLE001
        item["alive"] = False
    item["cost"] = time.time() - start
    return item


def probe_all(items: list[dict]) -> list[dict]:
    with futures.ThreadPoolExecutor(max_workers=PROBE_WORKERS) as pool:
        return list(pool.map(probe, items))


def collect_hkmotw() -> list[dict]:
    """从 iptv-org 的 hk/mo/tw 里取指定频道，换成中文名。"""
    items: list[dict] = []
    for code in ("hk", "mo", "tw"):
        try:
            raw = fetch(IPTV_ORG + f"{code}.m3u")
        except Exception as exc:  # noqa: BLE001
            print(f"  [警告] iptv-org {code} 拉取失败：{exc}")
            continue
        for it in parse(raw):
            cn = HKMOTW.get(it["tvg_id"])
            if not cn or not it["url"]:
                continue
            if "[Geo-blocked]" in it["raw_name"] or it["group"].lower() == "shop":
                continue
            items.append({
                "name": cn,
                "url": it["url"],
                "logo": it["logo"],
                "backup": HKMOTW_BACKUP.get(cn, []),
            })
    print(f"  港澳台命中 {len(items)} 条")
    return items


def hkmotw_blocks(items: list[dict]) -> list[str]:
    """按固定顺序输出港澳台，每个台主线路 + 备用线路（最多 3 条）。"""
    by_name = {it["name"]: it for it in items}
    blocks = []
    for name in HKMOTW_ORDER:
        it = by_name.get(name)
        if not it:
            continue
        urls = [it["url"]] + [u for u in it.get("backup", []) if u != it["url"]]
        urls = list(dict.fromkeys(urls))[:3]
        for url in urls:
            blocks.append(
                f'#EXTINF:-1 tvg-id="{name}" tvg-name="{name}" tvg-logo="{it["logo"]}" '
                f'group-title="{HKMOTW_GROUP}",{name}\n{url}'
            )
    return blocks


def sort_blocks(blocks: list[str]) -> list[str]:
    """按 channel_sort_key 重排整份文件（同频道内保持原线路顺序）。"""
    def name_of(block: str) -> str:
        first = block.splitlines()[0]
        return first.split(",")[-1].strip() if "," in first else first

    buckets: dict[str, list[str]] = {}
    for b in blocks:
        buckets.setdefault(name_of(b), []).append(b)
    ordered = []
    for name in sorted(buckets, key=channel_sort_key):
        ordered.extend(buckets[name])
    return ordered


def fill_missing_hkmotw(fresh: list[str], old_path: pathlib.Path) -> list[str]:
    """上游某个地区拉挂时，用旧文件补回缺失的港澳台频道。"""
    names = {b.splitlines()[0].split(",")[-1].strip() for b in fresh}
    missing = [n for n in HKMOTW_ORDER if n not in names]
    if not missing or not old_path.exists():
        return fresh
    old = old_path.read_text(encoding="utf-8").splitlines()
    recovered = []
    for i, ln in enumerate(old):
        if f'group-title="{HKMOTW_GROUP}"' in ln:
            name = ln.split(",")[-1].strip()
            if name in missing and i + 1 < len(old):
                recovered.append(f"{ln}\n{old[i + 1]}")
    if recovered:
        print(f"  [警告] 用旧文件补回 {len(recovered)} 条港澳台")
    return fresh + recovered


def build(items: list[dict]) -> None:
    by_channel: dict[str, list[dict]] = {}
    for it in items:
        by_channel.setdefault(it["std"], []).append(it)

    blocks = []
    for std in sorted(by_channel, key=channel_sort_key):
        lines = sorted(
            by_channel[std],
            key=lambda x: (not x.get("alive", False), x.get("cost", 99)),
        )[:MAX_LINES]
        group = "央视" if std.startswith(("CCTV", "CETV")) else "卫视"
        for it in lines:
            logo = it["logo"] or f"{FM_RAW}/tv/{std.upper()}.png"
            blocks.append(
                f'#EXTINF:-1 tvg-id="{std}" tvg-name="{std}" tvg-logo="{logo}" '
                f'group-title="{group}",{std}\n{it["url"]}'
            )

    header = f'#EXTM3U x-tvg-url="{FM_RAW}/e.xml"'
    DIST.mkdir(parents=True, exist_ok=True)
    out = DIST / "pack-cctv-satellite.m3u"
    existing = {b.splitlines()[-1] for b in blocks}
    hk_blocks = [b for b in hkmotw_blocks(collect_hkmotw()) if b.splitlines()[-1] not in existing]
    hk_blocks = fill_missing_hkmotw(hk_blocks, out)
    blocks.extend(hk_blocks)
    out.write_text("\n".join([header] + blocks) + "\n", encoding="utf-8")
    print(f"  pack-cctv-satellite.m3u: {len(by_channel)} 个频道 + 港澳台 {len(hk_blocks)} 条 / 共 {len(blocks)} 条线路")

    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / out.name).write_bytes(out.read_bytes())


def merge_hkmotw_only() -> int:
    """在现有包末尾追加港澳台，不动已探测好的央视/卫视线路。"""
    out = DIST / "pack-cctv-satellite.m3u"
    lines = out.read_text(encoding="utf-8").splitlines()
    header = lines[0]
    body = [ln for ln in lines[1:] if ln.strip()]
    blocks, cur = [], []
    for ln in body:
        if ln.startswith("#EXTINF"):
            if cur:
                blocks.append("\n".join(cur))
            cur = [ln]
        elif ln.startswith("#"):
            cur.append(ln)
        else:
            cur.append(ln)
            blocks.append("\n".join(cur))
            cur = []
    if cur:
        blocks.append("\n".join(cur))
    old_hk = [b for b in blocks if f'group-title="{HKMOTW_GROUP}"' in b]
    blocks = [b for b in blocks if f'group-title="{HKMOTW_GROUP}"' not in b]
    existing = {b.splitlines()[-1] for b in blocks}
    fresh = hkmotw_blocks(collect_hkmotw())
    fresh = fill_missing_hkmotw(fresh, out)
    hk_blocks = [b for b in fresh if b.splitlines()[-1] not in existing]
    blocks = sort_blocks(blocks + hk_blocks)
    out.write_text("\n".join([header] + blocks) + "\n", encoding="utf-8")
    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / out.name).write_bytes(out.read_bytes())
    print(f"  追加港澳台 {len(hk_blocks)} 条，当前共 {len(blocks) + len(hk_blocks)} 条线路")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--no-probe", action="store_true")
    ap.add_argument("--hkmotw-only", action="store_true")
    ap.add_argument("--reorder-only", action="store_true")
    args = ap.parse_args()

    if args.reorder_only:
        out = DIST / "pack-cctv-satellite.m3u"
        lines = out.read_text(encoding="utf-8").splitlines()
        header, blocks, cur = lines[0], [], []
        for ln in lines[1:]:
            if not ln.strip():
                continue
            if ln.startswith("#EXTINF"):
                if cur:
                    blocks.append("\n".join(cur))
                cur = [ln]
            else:
                cur.append(ln)
                if not ln.startswith("#"):
                    blocks.append("\n".join(cur))
                    cur = []
        if cur:
            blocks.append("\n".join(cur))
        out.write_text("\n".join([header] + sort_blocks(blocks)) + "\n", encoding="utf-8")
        (ASSETS / out.name).write_bytes(out.read_bytes())
        print(f"  重排完成，共 {len(blocks)} 条线路")
        return 0

    if args.hkmotw_only:
        return merge_hkmotw_only()

    print("收集候选源…")
    items = collect()
    print(f"  去重后 {len(items)} 条")

    if args.no_probe:
        for it in items:
            it["alive"] = False
    else:
        print(f"探测 {len(items)} 条（{PROBE_WORKERS} 并发，超时 {PROBE_TIMEOUT}s）…")
        items = probe_all(items)
        print(f"  存活 {sum(1 for i in items if i['alive'])} 条")

    build(items)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
