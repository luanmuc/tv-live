"""频道归一、显示名与排序。

别名表直接读 APK 内同一份 `channel_name_alias.json`，避免 Python 和
Kotlin 各维护一套；CCTV 显示全称和排序表仍留在构建侧，只影响 m3u 输出。
"""

from __future__ import annotations

import json
import re

from .common import ROOT

try:
    from hkmotw_ids import HKMOTW_ORDER
except ImportError:  # 作为包被导入时（tests / -m）从 scripts 目录兜底
    from pathlib import Path
    import sys

    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from hkmotw_ids import HKMOTW_ORDER  # type: ignore[no-redef]

CCTV_DISPLAY = {
    "CCTV1": "CCTV-1 综合",
    "CCTV2": "CCTV-2 财经",
    "CCTV3": "CCTV-3 综艺",
    "CCTV4": "CCTV-4 中文国际",
    "CCTV5": "CCTV-5 体育",
    "CCTV5+": "CCTV-5+ 体育赛事",
    "CCTV6": "CCTV-6 电影",
    "CCTV7": "CCTV-7 国防军事",
    "CCTV8": "CCTV-8 电视剧",
    "CCTV9": "CCTV-9 纪录",
    "CCTV10": "CCTV-10 科教",
    "CCTV11": "CCTV-11 戏曲",
    "CCTV12": "CCTV-12 社会与法",
    "CCTV13": "CCTV-13 新闻",
    "CCTV14": "CCTV-14 少儿",
    "CCTV15": "CCTV-15 音乐",
    "CCTV16": "CCTV-16 奥林匹克",
    "CCTV17": "CCTV-17 农业农村",
    "CETV1": "CETV-1 中国教育一台",
    "CETV2": "CETV-2 中国教育二台",
    "CETV4": "CETV-4 中国教育四台",
}

CHANNEL_ALIAS_FILE = ROOT / "core" / "data" / "src" / "main" / "res" / "raw" / "channel_name_alias.json"


def _normalize_alias(raw: str) -> str:
    key = re.sub(r"[\s\-_·「」]+", "", raw).strip().lower()
    for junk in ("高清", "超清", "标清", "蓝光", "4k", "8k", "uhd", "fhd", "hd"):
        key = key.replace(junk, "")
    return re.sub(r"(移动|电信|联通|专享)$", "", key)


def _load_channel_aliases() -> dict[str, str]:
    data = json.loads(CHANNEL_ALIAS_FILE.read_text(encoding="utf-8"))
    result: dict[str, str] = {}
    for standard, aliases in data.items():
        if standard.startswith("__"):
            continue
        for alias in [standard, *aliases]:
            result.setdefault(_normalize_alias(alias), standard)
    return result


CHANNEL_ALIAS = _load_channel_aliases()

KIDS_ORDER = ["CCTV14", "金鹰卡通", "卡酷少儿", "优漫卡通", "嘉佳卡通", "哈哈炫动", "炫动卡通", "动漫秀场"]
KIDS_CHANNELS = set(KIDS_ORDER)

# 卫视显示顺序：先一线卫视，再按大区，最后教育台
SATELLITE_ORDER = [
    "湖南卫视", "浙江卫视", "江苏卫视", "东方卫视", "北京卫视", "深圳卫视", "广东卫视",
    "安徽卫视", "山东卫视", "湖北卫视", "天津卫视", "四川卫视", "重庆卫视", "河南卫视",
    "河北卫视", "辽宁卫视", "黑龙江卫视", "吉林卫视", "江西卫视", "东南卫视", "贵州卫视",
    "云南卫视", "广西卫视", "山西卫视", "陕西卫视", "甘肃卫视", "宁夏卫视", "青海卫视",
    "新疆卫视", "西藏卫视", "内蒙古卫视", "海南卫视", "三沙卫视", "厦门卫视", "延边卫视",
    "康巴卫视", "安多卫视", "CETV1", "CETV2", "CETV4",
]


def display_name(std: str) -> str:
    """tvg-name/tvg-id 保持标准名给 EPG，逗号后的显示名用中文全称。"""
    return CCTV_DISPLAY.get(std, std)


def extinf_line(std: str, name: str, logo: str, group: str, item: dict) -> str:
    """生成 EXTINF，并把源清单里的 User-Agent 等属性带回去。"""
    ua = (item.get("http_user_agent") or "").replace('"', "")
    ua_attr = f' http-user-agent="{ua}"' if ua else ""
    return (
        f'#EXTINF:-1 tvg-id="{std}" tvg-name="{std}" tvg-logo="{logo}"'
        f"{ua_attr} group-title=\"{group}\",{name}"
    )


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
    if std in KIDS_ORDER:
        return (2, KIDS_ORDER.index(std), 0, std)
    if std in SATELLITE_ORDER:
        return (3, SATELLITE_ORDER.index(std), 0, std)
    if std in HKMOTW_ORDER:
        return (4, HKMOTW_ORDER.index(std), 0, std)
    return (5, 0, 0, std)


def standard_name(raw: str) -> str | None:
    """把别名收敛成标准频道名；别名表来自 APK 同一份 channel_name_alias.json。"""
    name = re.sub(r"[\(（\[【].*?[\)）\]】]", "", raw).strip()
    key = _normalize_alias(name)

    m = re.match(r"^cctv(\d{1,2})(\+)?$", key)
    if m:
        return f"CCTV{m.group(1)}{'+' if m.group(2) else ''}"
    return CHANNEL_ALIAS.get(key)
