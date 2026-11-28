"""流水线共享路径、并发参数与运行期统计容器。"""

from __future__ import annotations

import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
DIST = ROOT / "sources" / "dist"
ASSETS = ROOT / "core" / "data" / "src" / "main" / "assets" / "sources"

FM_RAW = "https://raw.githubusercontent.com/fanmingming/live/main"
FM = FM_RAW + "/tv/m3u/"
IPTV_ORG = "https://iptv-org.github.io/iptv/countries/"

UA = "okhttp/4.12.0"
MAX_LINES = 12
HK_MAX_LINES = 6
PROBE_TIMEOUT = 8
PROBE_WORKERS = 64
FETCH_TIMEOUT = 30
DEEP_PROBE_WORKERS = 32

SOURCE_STATS: dict[str, dict] = {}
AUDIT_SCORES: dict[str, int] = {}
