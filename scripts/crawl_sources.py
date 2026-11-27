#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""源发现爬虫：从公开源站页面/站点地图里找 m3u、m3u8、txt 直播源地址。

不是全网无差别爬取，而是围绕已知专业源站做「种子页 -> 链接 -> 校验」：
  1. 抓取种子页/sitemap
  2. 提取页面里的 m3u / m3u8 / txt 链接，拆掉 APTV 跳转壳
  3. 逐个拉前 4KB 校验，必须是真实播放列表，HTML 404 页会被丢掉
  4. 合并最近 7 天已验证的地址，写入 sources/discovered.json

产物由 scripts/build_pack.py 读取，作为固定入口之外的动态候选源。
"""

from __future__ import annotations

import concurrent.futures as futures
import datetime
import json
import pathlib
import re
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "sources" / "discovered.json"
UA = "Mozilla/5.0 (compatible; tv-live-crawler/1.0)"
MAX_SOURCES = 40
MAX_PAGE_BYTES = 768 * 1024
TIMEOUT = 20
KEEP_DAYS = 7

SEED_PAGES = [
    "https://m3u.ibert.me/",
    "https://live.zbds.top/",
    "https://profile.aptv.app/",
    "https://iptv.hacks.tools/domestic/IPv4%20%E7%9B%B4%E6%92%AD%E6%BA%90/all/txt",
    "https://iptv.hacks.tools/domestic/IPv4%20%E7%9B%B4%E6%92%AD%E6%BA%90/category/%E5%8D%AB%E8%A7%86%E9%A2%91%E9%81%93/txt",
]

# 这些不是源，或者只是播放器/台标/EPG
SKIP_WORDS = (
    "epg", "xml", "logo", "icon", "favicon", "player", "apk",
    "成人", "情色", "福利", "porn", "xxx", "18+",
)
WRAPPER_PREFIXES = (
    "https://add.aptv.app/",
    "http://add.aptv.app/",
    "https://gh.aptv.app/",
    "http://gh.aptv.app/",
)


def unwrap(url: str) -> str:
    """拆掉 APTV 的 add/gh 跳转前缀，拿到真实源地址。"""
    for _ in range(4):
        changed = False
        for prefix in WRAPPER_PREFIXES:
            if url.startswith(prefix):
                url = url[len(prefix):]
                changed = True
        if not changed:
            break
    return url


def fetch_text(url: str, limit: int = MAX_PAGE_BYTES) -> str:
    safe = ":/?&=#%+;,@!$'()*[]~"
    req = urllib.request.Request(
        urllib.parse.quote(url, safe=safe),
        headers={"User-Agent": UA, "Accept": "text/html,application/xml,text/plain,*/*"},
    )
    with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
        return resp.read(limit).decode("utf-8", errors="replace")


def extract_links(page: str, base: str) -> set[str]:
    """从 HTML 属性、裸文本、sitemap loc 中提取候选播放列表地址。"""
    found: set[str] = set()
    for raw in re.findall(r'(?:href|src)=["\']([^"\']+)["\']', page, re.I):
        found.add(raw)
    for raw in re.findall(r'https?://[^\s"\'<>\\]+', page):
        found.add(raw)
    for raw in re.findall(r'<loc>\s*([^<]+?)\s*</loc>', page, re.I):
        found.add(raw)

    out: set[str] = set()
    for raw in found:
        raw = raw.replace("&amp;", "&").strip()
        if not raw or raw.startswith(("mailto:", "javascript:", "#")):
            continue
        url = urllib.parse.urljoin(base, raw)
        url = unwrap(url)
        if not url.startswith(("http://", "https://")):
            continue
        path = urllib.parse.urlparse(url).path.lower()
        if not path.endswith((".m3u", ".m3u8", ".txt")):
            continue
        low = url.lower()
        if any(w in low for w in SKIP_WORDS):
            continue
        out.add(url.split("#", 1)[0])
    return out


def looks_like_feed(url: str) -> bool:
    if any(w in url.lower() for w in SKIP_WORDS):
        return False
    try:
        safe = ":/?&=#%+;,@!$'()*[]~"
        req = urllib.request.Request(
            urllib.parse.quote(url, safe=safe),
            headers={"User-Agent": UA, "Range": "bytes=0-4095"},
        )
        with urllib.request.urlopen(req, timeout=12) as resp:
            if resp.status not in (200, 206):
                return False
            ctype = (resp.headers.get("Content-Type") or "").lower()
            if "html" in ctype:
                return False
            body = resp.read(4096)
    except Exception:
        return False
    text = body.decode("utf-8", errors="replace").lstrip("\ufeff \r\n\t")
    if text.startswith("#EXTM3U") or "#EXTINF" in text[:4096]:
        return True
    # TXT 源：至少有「频道名,http」这样的行
    for line in text.splitlines()[:30]:
        line = line.strip()
        if line and not line.startswith("#") and "," in line and "http" in line.split(",", 1)[1]:
            return True
    return False


def load_old() -> list[dict]:
    try:
        payload = json.loads(OUT.read_text(encoding="utf-8"))
    except Exception:
        return []
    now = datetime.datetime.now(datetime.timezone.utc)
    kept = []
    for item in payload.get("sources", []):
        url = item.get("url", "")
        if not url or any(w in url.lower() for w in SKIP_WORDS):
            continue
        try:
            seen = datetime.datetime.fromisoformat(item.get("last_ok", ""))
        except Exception:
            continue
        if (now - seen).days > KEEP_DAYS:
            continue
        kept.append(item)
    return kept


def main() -> int:
    seed_links: dict[str, str] = {}
    errors: list[dict] = []

    for page in SEED_PAGES:
        try:
            html = fetch_text(page)
            links = extract_links(html, page)
            print(f"  {page}: 发现 {len(links)} 个候选")
            for link in links:
                seed_links.setdefault(link, page)
        except Exception as exc:  # noqa: BLE001
            print(f"  [警告] 种子页失败 {page}: {exc}")
            errors.append({"page": page, "error": str(exc)[:240]})

    # 旧地址先保留，避免某天种子页临时挂掉就把可用源全丢了
    old = {item["url"]: item for item in load_old()}
    candidates = list(seed_links) + [u for u in old if u not in seed_links]

    # 固定入口已经在 build_pack.py 里，爬虫只补“新地址”，避免重复抓取
    try:
        from build_pack import CANDIDATES
        fixed = {u for _, u in CANDIDATES}
    except Exception:
        fixed = set()

    seen: set[str] = set()
    queue: list[str] = []
    for url in candidates:
        if url in seen or url in fixed:
            continue
        seen.add(url)
        queue.append(url)

    with futures.ThreadPoolExecutor(max_workers=12) as pool:
        results = list(pool.map(looks_like_feed, queue))

    valid = [u for u, ok in zip(queue, results) if ok]
    grouped: dict[str, list[str]] = {}
    for url in valid:
        host = urllib.parse.urlparse(url).hostname or "unknown"
        grouped.setdefault(host, []).append(url)

    # 按域名轮转，避免 ibert 的 37 个地址把名额全吃光
    accepted: list[dict] = []
    hosts = list(grouped)
    idx = 0
    while len(accepted) < MAX_SOURCES and any(grouped[h] for h in hosts):
        host = hosts[idx % len(hosts)]
        if grouped[host] and sum(1 for x in accepted if urllib.parse.urlparse(x["url"]).hostname == host) < 10:
            url = grouped[host].pop(0)
            now = datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds")
            accepted.append({
                "url": url,
                "from": seed_links.get(url, old.get(url, {}).get("from", "")),
                "first_seen": old.get(url, {}).get("first_seen", now),
                "last_ok": now,
            })
            print(f"  [OK] {url}")
        idx += 1
        if idx > 10000:
            break

    payload = {
        "generated_at": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "seed_pages": SEED_PAGES,
        "errors": errors,
        "sources": accepted,
        "summary": {
            "seed_pages": len(SEED_PAGES),
            "discovered": len(seed_links),
            "accepted": len(accepted),
        },
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"源发现 -> {OUT.relative_to(ROOT)}  接受 {len(accepted)} 个")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
