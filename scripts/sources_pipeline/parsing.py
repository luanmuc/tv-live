"""上游文本抓取与 m3u/TXT 解析。"""

from __future__ import annotations

import re
import urllib.parse
import urllib.request

from .common import UA


def fetch(url: str, timeout: int = 60) -> str:
    # 中文分类路径必须先 percent-encode，urllib 不接受裸 UTF-8 URL。
    safe = ":/?&=#%+;,@!$'()*[]~"
    req = urllib.request.Request(urllib.parse.quote(url, safe=safe), headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read().decode("utf-8", errors="replace")


def clean_url(raw: str) -> str:
    """去掉上游清单里的注释尾巴（|Header=、$标记、『线路』、空格等）。"""
    u = raw.strip()
    for sep in ("|", "$", "『", " ", "\t"):
        u = u.split(sep, 1)[0]
    return u.strip()


def valid_url(u: str) -> bool:
    if not u.startswith(("http://", "https://")):
        return False
    return all(ord(c) < 128 for c in u) and " " not in u


def parse_txt(text: str) -> list[dict]:
    """解析「频道名,播放地址」的 TXT 源（部分国内聚合站用这种格式）。"""
    items = []
    for line in text.splitlines():
        line = line.strip().lstrip("\ufeff")
        if not line or line.startswith("#") or "," not in line:
            continue
        name, url = line.rsplit(",", 1)
        name = re.sub(r"^\d+[.、\s]*", "", name.strip())
        url = clean_url(url)
        if not name or not valid_url(url):
            continue
        items.append({
            "raw_name": name,
            "tvg_id": "",
            "tvg_name": name,
            "logo": "",
            "group": "",
            "url": url,
        })
    return items


def parse(text: str) -> list[dict]:
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    if not lines:
        return []
    if not lines[0].startswith("#EXTM3U"):
        return parse_txt(text)
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
                "http_user_agent": (re.search(r'http-user-agent="(.*?)"', line) or [None, ""])[1],
                "directives": [],
            }
        elif line.startswith("#"):
            # 保留播放器需要的线路级元数据，输出时原样带回。
            if cur is not None and line.startswith(("#EXTVLCOPT", "#KODIPROP", "#EXTGRP")):
                cur["directives"].append(line)
            continue
        elif cur is not None:
            u = clean_url(line)
            if valid_url(u):
                cur["url"] = u
                items.append(cur)
            cur = None
    return items
