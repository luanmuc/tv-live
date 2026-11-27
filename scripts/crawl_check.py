#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""线路巡检爬虫：对最终源包逐条测可用性，并给 HLS 线路做清单+首分片深检。

与源发现爬虫分开：
  - crawl_sources.py 负责“找新源地址”
  - crawl_check.py   负责“验证现有线路到底能不能播”

产物：sources/dist/line-audit.json
"""

from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import sys
import urllib.parse

from build_pack import deep_probe_all, parse, probe_all, standard_name

ROOT = pathlib.Path(__file__).resolve().parents[1]
DIST = ROOT / "sources" / "dist"
DEFAULT_FILES = [
    DIST / "pack-cctv-satellite.m3u",
    DIST / "intl-hkmo-tw.m3u",
]


def load_items(paths: list[pathlib.Path], limit: int | None = None) -> list[dict]:
    items: list[dict] = []
    seen: set[str] = set()
    for path in paths:
        if not path.exists():
            print(f"  [警告] 文件不存在: {path}")
            continue
        for it in parse(path.read_text(encoding="utf-8", errors="replace")):
            url = it.get("url", "")
            if not url or url in seen:
                continue
            seen.add(url)
            it["std"] = standard_name(it.get("raw_name", "")) or standard_name(it.get("tvg_name", "")) or it.get("raw_name", "")
            it["source"] = path.name
            items.append(it)
            if limit and len(items) >= limit:
                return items
    return items


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--file", action="append", help="要巡检的 m3u 文件，可重复")
    parser.add_argument("--out", default=str(DIST / "line-audit.json"))
    parser.add_argument("--limit", type=int, default=None, help="只测前 N 条，调试用")
    args = parser.parse_args()

    paths = [pathlib.Path(p) for p in args.file] if args.file else DEFAULT_FILES
    items = load_items(paths, args.limit)
    if not items:
        print("没有可巡检的线路", file=sys.stderr)
        return 1

    print(f"巡检 {len(items)} 条线路…")
    items = probe_all(items)
    alive = [it for it in items if it.get("alive") is True]
    print(f"  存活 {len(alive)}/{len(items)}，开始 HLS 深检…")
    if alive:
        deep_probe_all(alive)

    rows = []
    for it in items:
        alive_flag = it.get("alive")
        playable = it.get("playable")
        if alive_flag is True and playable is True:
            score = 100
        elif alive_flag is True:
            score = 70
        elif alive_flag is None:
            score = 40
        else:
            score = 0
        rows.append({
            "channel": it.get("std") or it.get("raw_name", ""),
            "source": it.get("source", ""),
            "url": it.get("url", ""),
            "alive": alive_flag,
            "playable": playable,
            "kind": it.get("kind", ""),
            "status_code": it.get("status_code"),
            "latency_ms": round(1000 * float(it.get("cost", 0))),
            "route_score": score,
            "error": it.get("error", ""),
        })

    summary = {
        "total": len(rows),
        "alive": sum(1 for r in rows if r["alive"] is True),
        "unknown": sum(1 for r in rows if r["alive"] is None),
        "dead": sum(1 for r in rows if r["alive"] is False),
        "playable": sum(1 for r in rows if r["playable"] is True),
        "avg_route_score": round(sum(r["route_score"] for r in rows) / len(rows), 1),
    }
    payload = {
        "generated_at": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "files": [str(p.relative_to(ROOT)) if p.is_absolute() else str(p) for p in paths],
        "summary": summary,
        "lines": rows,
    }
    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"线路巡检 -> {out.relative_to(ROOT) if out.is_relative_to(ROOT) else out}  {summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
