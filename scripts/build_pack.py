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
import datetime
import json
import pathlib
import re
import time
import urllib.parse
import urllib.request

from sources_pipeline.channels import (
    KIDS_CHANNELS,
    channel_sort_key,
    display_name,
    extinf_line,
    standard_name,
)
from sources_pipeline.common import (
    ASSETS,
    AUDIT_SCORES,
    DEEP_PROBE_WORKERS,
    DIST,
    FETCH_TIMEOUT,
    FM,
    FM_RAW,
    HK_MAX_LINES,
    IPTV_ORG,
    MAX_LINES,
    PROBE_TIMEOUT,
    PROBE_WORKERS,
    ROOT,
    SOURCE_STATS,
)
from sources_pipeline.parsing import clean_url, fetch, parse, parse_txt, valid_url

SX_RAW = "https://raw.githubusercontent.com/suxuang/myIPTV/main/"
VB_RAW = "https://raw.githubusercontent.com/vbskycn/iptv/master/tv/"
YG_RAW = "https://raw.githubusercontent.com/YanG-1989/m3u/main/"

CANDIDATES = [
    ("fanmingming index", FM + "index.m3u"),
    ("fanmingming itv", FM + "itv.m3u"),
    ("fanmingming ipv6", FM + "ipv6.m3u"),
    ("iptv-org cn", IPTV_ORG + "cn.m3u"),
    ("iptv-org zho", "https://iptv-org.github.io/iptv/languages/zho.m3u"),
    ("suxiang ipv4", SX_RAW + "ipv4.m3u"),
    ("suxiang ipv6", SX_RAW + "ipv6.m3u"),
    ("vbskycn ipv4", VB_RAW + "iptv4.m3u"),
    ("yang gather", YG_RAW + "Gather.m3u"),
    ("guovin result", "https://raw.githubusercontent.com/Guovin/iptv-api/gd/output/result.m3u"),
    ("wwb521 live", "https://raw.githubusercontent.com/wwb521/live/main/tv.m3u"),
    ("peterhchina v4", "https://peterhchina.github.io/iptv/IPTV-V4.m3u"),
    ("et6jt3 ipv4", "https://ET6jt3.github.io/iptv/tv/iptv4.m3u"),
    ("yonsm iptv", "https://yonsm.github.io/IPTV/iptv.m3u8"),
    # 非 GitHub 的国内聚合站 / 自动更新站
    ("zbds iptv4", "https://live.zbds.top/tv/iptv4.m3u"),
    ("ibert cn", "https://m3u.ibert.me/cn.m3u"),
    ("ibert o_s_cn", "https://m3u.ibert.me/o_s_cn.m3u"),
    ("ibert o_all", "https://m3u.ibert.me/o_all.m3u"),
    ("ibert fmml_itv", "https://m3u.ibert.me/fmml_itv.m3u"),
    # hacks.tools / live.hacks.tools：非 GitHub、每 6 小时更新的国内分类源
    ("hacks cctv", "https://live.hacks.tools/tv/ipv4/categories/央视频道.txt"),
    ("hacks satellite", "https://live.hacks.tools/tv/ipv4/categories/卫视频道.txt"),
    ("hacks local", "https://live.hacks.tools/tv/ipv4/categories/地方频道.txt"),
    ("hacks sports", "https://live.hacks.tools/tv/ipv4/categories/体育频道.txt"),
    ("hacks movie", "https://live.hacks.tools/tv/ipv4/categories/电影频道.txt"),
    ("hacks documentary", "https://live.hacks.tools/tv/ipv4/categories/纪录频道.txt"),
    ("hacks music", "https://live.hacks.tools/tv/ipv4/categories/音乐频道.txt"),
    ("hacks opera", "https://live.hacks.tools/tv/ipv4/categories/戏曲频道.txt"),
    ("hacks game", "https://live.hacks.tools/tv/ipv4/categories/游戏频道.txt"),
    # 裤佬：非 GitHub 的每日直播源（TXT/M3U 混合内容）
    ("kulao live", "https://iptv.445569.xyz/live.m3u"),
]


def _crawl_key(url: str) -> str:
    """把 ibert 的 /txt/foo.txt 归一成 /foo.m3u，避免同一份源抓两遍。"""
    parts = urllib.parse.urlsplit(url)
    path = re.sub(r"/txt/([^/]+)\.txt$", r"/\1.m3u", parts.path)
    return urllib.parse.urlunsplit((parts.scheme, parts.netloc, path, "", ""))


def discovered_candidates(limit: int = 24) -> list[tuple[str, str]]:
    """读取 crawl_sources.py 发现的地址，作为固定入口之外的动态候选。"""
    path = ROOT / "sources" / "discovered.json"
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return []

    fixed = {_crawl_key(u) for _, u in CANDIDATES}
    grouped: dict[str, list[str]] = {}
    seen: set[str] = set()
    for item in payload.get("sources", []):
        url = item.get("url", "")
        if not url:
            continue
        key = _crawl_key(url)
        if key in fixed or key in seen:
            continue
        seen.add(key)
        host = urllib.parse.urlparse(url).hostname or "unknown"
        if host in {"gh-proxy.com", "gh.aptv.app", "add.aptv.app"}:
            continue
        grouped.setdefault(host, []).append(url)

    # 同一个站最多取 6 个，按域名轮转，避免 24 个名额全被 ibert 占掉
    out: list[tuple[str, str]] = []
    hosts = list(grouped)
    idx = 0
    while len(out) < limit and any(grouped[h] for h in hosts):
        host = hosts[idx % len(hosts)]
        if grouped[host] and sum(1 for _, u in out if urllib.parse.urlparse(u).hostname == host) < 6:
            url = grouped[host].pop(0)
            out.append((f"crawl {host}#{len(out) + 1}", url))
        idx += 1
        if idx > 10000:
            break
    return out


def collect() -> list[dict]:
    pool: dict[str, dict] = {}
    SOURCE_STATS.clear()
    candidates = list(CANDIDATES) + discovered_candidates()

    # 50+ 个入口串行拉取会被慢站拖到几十分钟；并发抓取，但仍按入口顺序合并，
    # 这样重复线路的归属和 source-stats 统计保持稳定。
    def fetch_one(pair: tuple[str, str]) -> tuple[str, str, str | None, str]:
        label, url = pair
        try:
            return label, url, fetch(url, timeout=FETCH_TIMEOUT), ""
        except Exception as exc:  # noqa: BLE001
            return label, url, None, str(exc)[:300]

    with futures.ThreadPoolExecutor(max_workers=8) as pool_exec:
        fetched = list(pool_exec.map(fetch_one, candidates))

    for label, url, text, error in fetched:
        st = {
            "label": label,
            "url": url,
            "fetched": 0,
            "matched": 0,
            "new": 0,
            "duplicate": 0,
            "error": "",
        }
        SOURCE_STATS[label] = st
        if text is None:
            st["error"] = error
            print(f"  [警告] {label} 拉取失败：{error}")
            continue
        try:
            items = parse(text)
        except Exception as exc:  # noqa: BLE001
            st["error"] = str(exc)[:300]
            print(f"  [警告] {label} 解析失败：{exc}")
            continue
        st["fetched"] = len(items)
        kept = 0
        for it in items:
            std = standard_name(it["raw_name"]) or standard_name(it["tvg_name"])
            if not std or not it["url"] or it["url"].startswith("rtp"):
                continue
            st["matched"] += 1
            it["std"] = std
            it["source"] = label
            it["audit_score"] = AUDIT_SCORES.get(it["url"], 0)
            if it["url"] in pool:
                st["duplicate"] += 1
                continue
            pool[it["url"]] = it
            st["new"] += 1
            kept += 1
        print(
            f"  {label}: {len(items)} 条，新增 {kept} 条"
            f"（匹配 {st['matched']}，重复 {st['duplicate']}）"
        )
    return list(pool.values())


def finalize_source_stats(items: list[dict]) -> None:
    """按源统计贡献、存活率和重复率，写入 sources/dist/source-stats.json。"""
    DIST.mkdir(parents=True, exist_ok=True)
    for label, st in SOURCE_STATS.items():
        src = [i for i in items if i.get("source") == label]
        alive = [i for i in src if i.get("alive") is True]
        st["probed"] = len(src)
        st["alive"] = len(alive)
        st["unknown"] = sum(1 for i in src if i.get("alive") is None)
        st["dead"] = sum(1 for i in src if i.get("alive") is False)
        st["alive_rate"] = round(len(alive) / len(src), 4) if src else 0.0
        st["duplicate_rate"] = (
            round(st["duplicate"] / st["matched"], 4) if st["matched"] else 0.0
        )
        st["avg_latency_ms"] = (
            round(1000 * sum(float(i.get("cost", 0)) for i in alive) / len(alive))
            if alive else None
        )
        coverage = min(1.0, len(alive) / 40)
        # 样本太少的源不能因为“1/1 全活”排到前面
        confidence = min(1.0, len(src) / 20)
        score = (
            55 * st["alive_rate"] * confidence
            + 30 * coverage
            + 15 * (1 - st["duplicate_rate"])
            if not st["error"] else 0.0
        )
        st["score"] = round(score, 1)
        st["status"] = "ok" if st["new"] and not st["error"] else "fail"

    payload = {
        "generated_at": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "sources": sorted(SOURCE_STATS.values(), key=lambda x: -x["score"]),
    }
    out = DIST / "source-stats.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"  源评分 -> {out.relative_to(ROOT)}")
    for st in payload["sources"][:5]:
        print(f"    {st['score']:5.1f}  {st['label']}  可用 {st['alive']}/{st['probed']}")


def is_ipv6_url(url: str) -> bool:
    host = url.split("//", 1)[-1].split("/", 1)[0]
    return "[" in host or host.count(":") >= 2


def probe(item: dict) -> dict:
    """单条线路探测：HLS 看清单特征，直连 TS 看 Content-Type / 首字节 0x47。"""
    start = time.time()
    if is_ipv6_url(item["url"]):
        item["alive"] = None  # 本机无 IPv6，保留给有 IPv6 的电视盒子
        item["kind"] = "ipv6"
        item["cost"] = 50
        return item

    item["alive"] = False
    item["kind"] = "error"
    item["status_code"] = None
    item["content_type"] = ""
    item["error"] = ""
    for attempt in range(2):
        try:
            req = urllib.request.Request(
                item["url"],
                headers={"User-Agent": UA, "Range": "bytes=0-2048"},
            )
            with urllib.request.urlopen(req, timeout=PROBE_TIMEOUT) as resp:
                status = resp.status
                ctype = (resp.headers.get("Content-Type") or "").lower()
                body = resp.read(2048)
                is_hls = b"#EXTM3U" in body or b"#EXT-X-" in body
                is_ts = (
                    "video/mp2t" in ctype
                    or "video/mp4" in ctype
                    or (body[:1] == b"\x47")
                )
                item["status_code"] = status
                item["content_type"] = ctype[:80]
                item["kind"] = "hls" if is_hls else ("ts" if is_ts else "other")
                item["alive"] = status in (200, 206) and (is_hls or is_ts)
                item["error"] = ""
                break
        except Exception as exc:  # noqa: BLE001
            item["error"] = str(exc)[:200]
            if attempt == 0:
                time.sleep(0.4)
                continue
            item["alive"] = False
            item["kind"] = "error"
    item["cost"] = time.time() - start
    return item


def _read_url(url: str, timeout: int = 6, limit: int = 512 * 1024) -> tuple[int, str, bytes]:
    req = urllib.request.Request(
        url,
        headers={"User-Agent": UA, "Range": f"bytes=0-{limit - 1}"},
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        ctype = (resp.headers.get("Content-Type") or "").lower()
        return resp.status, ctype, resp.read(limit)


def _first_media_url(text: str, base: str) -> str | None:
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        return urllib.parse.urljoin(base, line)
    return None


def deep_probe(item: dict) -> dict:
    """对已判活的 HLS 线路再拉一次清单和首个分片，确认不是空壳。"""
    item["playable"] = None
    if item.get("alive") is not True:
        return item
    if item.get("kind") != "hls":
        item["playable"] = True  # 直连 TS 已由首字节/Content-Type 证明
        return item

    try:
        status, _, body = _read_url(item["url"], timeout=6, limit=256 * 1024)
        text = body.decode("utf-8", errors="replace")
        if status not in (200, 206):
            item["playable"] = False
            return item

        media = _first_media_url(text, item["url"])
        if not media:
            item["playable"] = False
            return item

        # 主清单再下钻一层到媒体清单
        if "#EXT-X-STREAM-INF" in text or media.split("?")[0].endswith(".m3u8"):
            status2, _, body2 = _read_url(media, timeout=6, limit=256 * 1024)
            if status2 in (200, 206):
                media2 = _first_media_url(body2.decode("utf-8", errors="replace"), media)
                if media2:
                    media = media2

        status3, ctype3, seg = _read_url(media, timeout=6, limit=4096)
        item["media_url"] = media
        item["playable"] = status3 in (200, 206) and (
            bool(seg) or "video/" in ctype3 or "application/octet-stream" in ctype3
        )
    except Exception:  # noqa: BLE001
        item["playable"] = False
    return item


def deep_probe_all(items: list[dict]) -> list[dict]:
    with futures.ThreadPoolExecutor(max_workers=DEEP_PROBE_WORKERS) as pool:
        return list(pool.map(deep_probe, items))


def write_line_stats(items: list[dict], extra: list[dict] | None = None) -> None:
    DIST.mkdir(parents=True, exist_ok=True)
    rows = []
    for it in (items + (extra or [])):
        rows.append(
            {
                "channel": it.get("std") or it.get("name", ""),
                "source": it.get("source", "hkmotw"),
                "url": it.get("url", ""),
                "alive": it.get("alive"),
                "playable": it.get("playable"),
                "kind": it.get("kind", ""),
                "status_code": it.get("status_code"),
                "content_type": it.get("content_type", ""),
                "latency_ms": round(1000 * float(it.get("cost", 0))),
                "error": it.get("error", ""),
            }
        )
    payload = {
        "generated_at": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "lines": rows,
        "summary": {
            "total": len(rows),
            "alive": sum(1 for r in rows if r["alive"] is True),
            "unknown": sum(1 for r in rows if r["alive"] is None),
            "dead": sum(1 for r in rows if r["alive"] is False),
            "playable": sum(1 for r in rows if r["playable"] is True),
        },
    }
    out = DIST / "line-stats.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"  线路明细 -> {out.relative_to(ROOT)}  {payload['summary']}")


def probe_all(items: list[dict]) -> list[dict]:
    with futures.ThreadPoolExecutor(max_workers=PROBE_WORKERS) as pool:
        return list(pool.map(probe, items))


def host_of(url: str) -> str:
    return url.split("//", 1)[-1].split("/", 1)[0]


def load_audit_scores() -> None:
    """把上一轮 line-audit 的 route_score 作为选线提示；只做加分，不替代实测。"""
    AUDIT_SCORES.clear()
    path = DIST / "line-audit.json"
    if not path.exists():
        return
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001
        print(f"  [警告] 读取巡检结果失败：{exc}")
        return
    for row in payload.get("lines", []):
        url = row.get("url")
        if url:
            AUDIT_SCORES[url] = int(row.get("route_score", 0) or 0)
    print(f"  载入上一轮巡检提示 {len(AUDIT_SCORES)} 条")


def selection_rank(item: dict) -> tuple[int, int, float, str]:
    """选线排序：深检可拉 > 存活 > 未知 > 死链；同级参考上一轮巡检分、延迟、源名。"""
    if item.get("playable") is True:
        level = 0
    elif item.get("alive") is True:
        level = 1
    elif item.get("alive") is None:
        level = 2
    else:
        level = 3
    return (
        level,
        -int(item.get("audit_score", 0) or 0),
        float(item.get("cost", 99)),
        str(item.get("source", "")),
    )


def pick_lines(cands: list[dict], limit: int) -> list[dict]:
    """同频道选线：优先深检可拉，其次存活；完全没活线才留少量死链兜底。

    同一 host 最多 2 条，尽量用不同源；活线之后最多再带 2 条未知/死链，
    避免一份 12 条里 10 条都是死链。
    """
    ordered = sorted(cands, key=selection_rank)
    alive = [it for it in ordered if it.get("alive") is True]
    unknown = [it for it in ordered if it.get("alive") is None]
    dead = [it for it in ordered if it.get("alive") is False]

    picked: list[dict] = []
    host_count: dict[str, int] = {}

    def add(it: dict) -> bool:
        h = host_of(it["url"])
        if host_count.get(h, 0) >= 2:
            return False
        picked.append(it)
        host_count[h] = host_count.get(h, 0) + 1
        return True

    for it in alive:
        add(it)
        if len(picked) >= limit:
            return picked

    # 有活线：最多再带 2 条兜底；一条活线都没有：最多带 4 条备用
    fallback = 2 if picked else min(4, limit)
    for it in unknown + dead:
        if len(picked) >= limit or fallback <= 0:
            break
        if add(it):
            fallback -= 1
    return picked


HK_KEYWORDS = {
    "翡翠台": ["翡翠"],
    "凤凰卫视中文台": ["凤凰中文", "凤凰卫视中文"],
    "凤凰卫视资讯台": ["凤凰资讯", "凤凰卫视资讯"],
    "ViuTV": ["viutv"],
    "天映频道": ["天映"],
    "美亚电影台": ["美亚电影", "美亚"],
    "澳视澳门": ["澳视澳门", "澳门卫视"],
    "澳视资讯": ["澳视资讯"],
    "澳视体育": ["澳视体育"],
    "澳视娱乐": ["澳视娱乐"],
    "莲花卫视": ["莲花"],
    "台视": ["台视"],
    "台视新闻": ["台视新闻"],
    "华视": ["华视"],
    "中天综合台": ["中天综合", "中天"],
    "民视": ["民视"],
    "民视第一台": ["民视第一"],
    "东森新闻": ["东森新闻"],
    "东森财经新闻": ["东森财经"],
    "东森洋片台": ["东森洋片"],
    "TVBS-Asia": ["tvbs"],
    "公视三台": ["公视三"],
    "客家电视台": ["客家"],
    "原住民族电视台": ["原住民"],
    "大爱一台": ["大爱一"],
    "大爱二台": ["大爱二"],
    "好消息电视台": ["好消息"],
}
_HK_MATCH = sorted(
    ((kw, std) for std, kws in HK_KEYWORDS.items() for kw in kws),
    key=lambda x: -len(x[0]),
)


def match_hkmotw(raw: str) -> str | None:
    n = re.sub(r"[\s\-_·「」（）()]+", "", raw).lower()
    for kw, std in _HK_MATCH:
        if kw in n:
            return std
    return None


def collect_hkmotw() -> list[dict]:
    """港澳台候选：iptv-org 的 hk/mo/tw + 公开中文列表按台名匹配 + 静态备用。"""
    items: list[dict] = []
    seen: set[tuple[str, str]] = set()

    def add(name: str, url: str, logo: str = "") -> None:
        if not url or url.startswith("rtp"):
            return
        key = (name, url)
        if key in seen:
            return
        seen.add(key)
        items.append({"name": name, "url": url, "logo": logo, "source": "hkmotw"})

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
            add(cn, it["url"], it["logo"])

    extra = [
        ("suxiang ipv4", SX_RAW + "ipv4.m3u"),
        ("suxiang ipv6", SX_RAW + "ipv6.m3u"),
        ("vbskycn ipv4", VB_RAW + "iptv4.m3u"),
        ("yang gather", YG_RAW + "Gather.m3u"),
    ]
    for label, url in extra:
        try:
            text = fetch(url)
        except Exception as exc:  # noqa: BLE001
            print(f"  [警告] 港澳台补充源 {label} 拉取失败：{exc}")
            continue
        hit = 0
        for it in parse(text):
            if "[Geo-blocked]" in it["raw_name"] or it["group"].lower() == "shop":
                continue
            cn = match_hkmotw(it["raw_name"]) or match_hkmotw(it.get("tvg_name", ""))
            if not cn:
                continue
            add(cn, it["url"], it["logo"])
            hit += 1
        print(f"  {label}: 港澳台 +{hit}")

    for name, urls in HKMOTW_BACKUP.items():
        for u in urls:
            add(name, u)

    print(f"  港澳台候选 {len(items)} 条（{len({i['name'] for i in items})} 个台）")
    return items


def hkmotw_blocks(items: list[dict]) -> list[str]:
    """按固定顺序输出港澳台，每台最多 HK_MAX_LINES 条，存活优先。"""
    by_name: dict[str, list[dict]] = {}
    for it in items:
        by_name.setdefault(it["name"], []).append(it)

    blocks = []
    for name in HKMOTW_ORDER:
        cands = by_name.get(name)
        if not cands:
            continue
        pool = sorted(cands, key=selection_rank)[: HK_MAX_LINES * 2]
        deep_probe_all(pool)
        picked = pick_lines(pool, HK_MAX_LINES)
        logo = next((c.get("logo") for c in picked if c.get("logo")), "")
        for it in picked:
            block = [extinf_line(name, name, logo, HKMOTW_GROUP, it)]
            block.extend(it.get("directives", []))
            block.append(it["url"])
            blocks.append("\n".join(block))
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
    old_blocks: list[list[str]] = []
    cur: list[str] = []
    for line in old[1:]:
        if not line.strip():
            continue
        if line.startswith("#EXTINF"):
            if cur:
                old_blocks.append(cur)
            cur = [line]
        elif cur:
            cur.append(line)
    if cur:
        old_blocks.append(cur)

    recovered = []
    for block in old_blocks:
        if not block or f'group-title="{HKMOTW_GROUP}"' not in block[0]:
            continue
        name = block[0].split(",")[-1].strip()
        if name in missing and len(block) >= 2:
            recovered.append("\n".join(block))
    if recovered:
        print(f"  [警告] 用旧文件补回 {len(recovered)} 条港澳台")
    return fresh + recovered


def build(items: list[dict]) -> None:
    by_channel: dict[str, list[dict]] = {}
    for it in items:
        by_channel.setdefault(it["std"], []).append(it)

    # 先按存活/延迟取稍宽的候选池，深检后再按 playable 定稿。
    # 这样不会因为前 12 条恰好都是“能连上但分片拉不动”而把可播线路挤掉。
    pools: dict[str, list[dict]] = {}
    all_pool: list[dict] = []
    for std in sorted(by_channel, key=channel_sort_key):
        pool = sorted(by_channel[std], key=selection_rank)[: MAX_LINES + 6]
        pools[std] = pool
        all_pool.extend(pool)

    print(f"  深检候选 {len(all_pool)} 条…")
    deep_probe_all(all_pool)

    selected: dict[str, list[dict]] = {}
    for std in sorted(pools, key=channel_sort_key):
        selected[std] = pick_lines(pools[std], MAX_LINES)

    picked = [it for lines in selected.values() for it in lines]
    playable = sum(1 for it in picked if it.get("playable") is True)
    print(f"  分片可拉取 {playable}/{len(picked)} 条")

    blocks = []
    for std, lines in selected.items():
        if std in KIDS_CHANNELS:
            group = "少儿频道"
        elif std.startswith(("CCTV", "CETV")):
            group = "央视"
        else:
            group = "卫视"
        for it in lines:
            logo = it["logo"] or f"{FM_RAW}/tv/{std.upper()}.png"
            block = [
                extinf_line(std, display_name(std), logo, group, it),
            ]
            block.extend(it.get("directives", []))
            block.append(it["url"])
            blocks.append("\n".join(block))

    header = f'#EXTM3U x-tvg-url="{FM_RAW}/e.xml"'
    DIST.mkdir(parents=True, exist_ok=True)
    out = DIST / "pack-cctv-satellite.m3u"
    existing = {b.splitlines()[-1] for b in blocks}
    hk_items = probe_all(collect_hkmotw())
    hk_blocks = [b for b in hkmotw_blocks(hk_items) if b.splitlines()[-1] not in existing]
    hk_blocks = fill_missing_hkmotw(hk_blocks, out)
    blocks.extend(hk_blocks)
    out.write_text("\n".join([header] + blocks) + "\n", encoding="utf-8")
    print(f"  pack-cctv-satellite.m3u: {len(by_channel)} 个频道 + 港澳台 {len(hk_blocks)} 条 / 共 {len(blocks)} 条线路")

    final_urls = {b.splitlines()[-1] for b in blocks}
    final_lines = [it for it in (items + hk_items) if it.get("url") in final_urls]
    write_line_stats(final_lines)
    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / out.name).write_bytes(out.read_bytes())
    (ASSETS / "line-stats.json").write_bytes((DIST / "line-stats.json").read_bytes())


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
    fresh = hkmotw_blocks(probe_all(collect_hkmotw()))
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
    load_audit_scores()
    items = collect()
    print(f"  去重后 {len(items)} 条")

    if args.no_probe:
        for it in items:
            it["alive"] = False
    else:
        print(f"探测 {len(items)} 条（{PROBE_WORKERS} 并发，超时 {PROBE_TIMEOUT}s）…")
        items = probe_all(items)
        print(f"  存活 {sum(1 for i in items if i['alive'] is True)} 条")

    finalize_source_stats(items)
    build(items)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
