#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""港澳台并入包的统一映射：iptv-org 的 tvg-id → 中文台名。"""

# 港澳台并入央视/卫视包：只收这批有中文名的台，购物台与锁区台不要
HKMOTW = {
    # 香港
    "Jade.hk": "翡翠台",
    "PhoenixChineseChannel.hk": "凤凰卫视中文台",
    "PhoenixInfoNewsChannel.hk": "凤凰卫视资讯台",
    "ViuTV.hk": "ViuTV",
    "CelestialMovies.hk": "天映频道",
    "MeiAhMovieChannel.hk": "美亚电影台",
    # 澳门
    "TDMOuMun.mo": "澳视澳门",
    "TDMInformation.mo": "澳视资讯",
    "TDMSports.mo": "澳视体育",
    "TDMEntertainment.mo": "澳视娱乐",
    "LotusTV.mo": "莲花卫视",
    # 台湾
    "TTV.tw": "台视",
    "TTVNews.tw": "台视新闻",
    "CTS.tw": "华视",
    "CTiVariety.tw": "中天综合台",
    "FTV.tw": "民视",
    "FTVOne.tw": "民视第一台",
    "EBCNews.tw": "东森新闻",
    "EBCFinancialNews.tw": "东森财经新闻",
    "EBCForeignMovie.tw": "东森洋片台",
    "TVBSAsia.tw": "TVBS-Asia",
    "PTS3.tw": "公视三台",
    "HakkaTV.tw": "客家电视台",
    "IndigenousTV.tw": "原住民族电视台",
    "DaAi1.tw": "大爱一台",
    "DaAi2.tw": "大爱二台",
    "GoodTV.tw": "好消息电视台",
}

HKMOTW_GROUP = "港澳台"

# 港澳台显示顺序（按地区习惯：香港 → 澳门 → 台湾）
HKMOTW_ORDER = [
    "翡翠台", "凤凰卫视中文台", "凤凰卫视资讯台", "ViuTV", "天映频道", "美亚电影台",
    "澳视澳门", "澳视资讯", "澳视体育", "澳视娱乐", "莲花卫视",
    "台视", "台视新闻", "华视", "中天综合台", "民视", "民视第一台",
    "东森新闻", "东森财经新闻", "东森洋片台", "TVBS-Asia",
    "公视三台", "客家电视台", "原住民族电视台", "大爱一台", "大爱二台", "好消息电视台",
]

# 补的二路源（iptv-org 之外的公开线路，能用的优先，失效的由 App 自动跳过）
HKMOTW_BACKUP = {
    "翡翠台": [
        "http://r.jdshipin.com/62WM7",
    ],
    "凤凰卫视中文台": [
        "http://223.110.245.139/PLTV/3/224/3221226922/index.m3u8",
        "http://125.210.152.18:9090/live/FHZW_1200.m3u8",
    ],
    "凤凰卫视资讯台": [
        "http://125.210.152.18:9090/live/FHZX_1200.m3u8",
        "http://php.17186.eu.org/phtv/fhzx.m3u8",
    ],
    "美亚电影台": [
        "http://103.172.187.30:12000/stream/mytv/null-2/master.m3u8",
        "http://103.172.187.30:12000/stream/mytv/null-9/master.m3u8",
    ],
    "澳视澳门": [
        "https://live3.tdm.com.mo/ch1/ch1.live/playlist.m3u8",
        "http://61.244.22.4/ch1/ch1.live/playlist.m3u8",
    ],
    "莲花卫视": [
        "https://cdn4.skygo.mn/live/disk1/Lotus/HLSv3-FTA/Lotus.m3u8",
    ],
    "台视": [
        "http://38.64.72.148/hls/modn/list/4013/chunklist1.m3u8",
    ],
    "台视新闻": [
        "https://live.wcetv.com/hls/master-ttvnews.m3u8",
        "http://38.64.72.148/hls/modn/list/4013/chunklist1.m3u8",
    ],
    "TVBS-Asia": [
        "http://38.64.72.148/hls/modn/list/4005/playlist.m3u8",
    ],
    "大爱二台": [
        "https://pulltv2.wanfudaluye.com/live/tv2.m3u8",
    ],
}
