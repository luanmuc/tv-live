"""直播源打包流水线。

抓取、频道归一、探测、选线和输出按职责拆开；`scripts/build_pack.py`
保留原来的 CLI，旧调用方不需要改参数。
"""

from .channels import (
    CCTV_DISPLAY,
    KIDS_CHANNELS,
    KIDS_ORDER,
    SATELLITE_ORDER,
    channel_sort_key,
    display_name,
    extinf_line,
    standard_name,
)
from .common import (
    ASSETS,
    AUDIT_SCORES,
    DIST,
    DEEP_PROBE_WORKERS,
    FETCH_TIMEOUT,
    HK_MAX_LINES,
    MAX_LINES,
    PROBE_TIMEOUT,
    PROBE_WORKERS,
    ROOT,
    SOURCE_STATS,
    UA,
)
from .parsing import clean_url, fetch, parse, parse_txt, valid_url

__all__ = [
    "ASSETS",
    "AUDIT_SCORES",
    "CCTV_DISPLAY",
    "DEEP_PROBE_WORKERS",
    "DIST",
    "FETCH_TIMEOUT",
    "HK_MAX_LINES",
    "KIDS_CHANNELS",
    "KIDS_ORDER",
    "MAX_LINES",
    "PROBE_TIMEOUT",
    "PROBE_WORKERS",
    "ROOT",
    "SATELLITE_ORDER",
    "SOURCE_STATS",
    "UA",
    "channel_sort_key",
    "clean_url",
    "display_name",
    "extinf_line",
    "fetch",
    "parse",
    "parse_txt",
    "standard_name",
    "valid_url",
]
