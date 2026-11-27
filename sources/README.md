# sources

内置直播源的唯一维护位置。

```
sources/dist/                       # 源快照，进 git
├── pack-cctv-satellite.m3u         # 央视 + 卫视 + 港澳台多线路包（92 频道 / 735 线路）
├── intl-hkmo-tw.m3u                # 港澳台及国际（189 个，已去重）
├── source-stats.json               # 每个上游的贡献/存活率/重复率/评分
├── line-stats.json                 # 央视/卫视/港澳台每条线路的存活、类型、延迟、分片
└── intl-line-stats.json            # 国际源每条线路的存活、类型、延迟

core/data/src/main/assets/sources/  # 打包进 APK 的副本（脚本自动同步，勿手改）
```

## 来源

| 文件 | 生成方式 | 说明 |
| --- | --- | --- |
| `pack-cctv-satellite.m3u` | `scripts/build_pack.py` | 合并 19 个入口（GitHub 聚合仓库 + live.zbds.top、m3u.ibert.me 等国内自动更新站），按频道名归一；只保留活线，每频道最多再带 2 条兜底，整台无活线才留 4 条 |
| `intl-hkmo-tw.m3u` | `scripts/refresh_sources.py` | iptv-org `countries/{cn,hk,mo,tw}.m3u` 合并去重，并逐条探测后按存活/延迟排序 |
| `source-stats.json` | `scripts/build_pack.py` | 上游级评分：抓取数、匹配数、新增数、重复率、存活率、平均延迟 |
| `line-stats.json` | `scripts/build_pack.py` | 线路级明细：HLS/TS/错误、HTTP 状态、延迟、首个分片是否可拉取 |

## 命名与字段约定

App 的 m3u 解析器按下面这套取值（`M3uIptvParser.kt`），改源时要保持：

- `#EXTM3U x-tvg-url="..."`：该源默认节目单，App 会自动读
- `tvg-name`：节目单匹配用的频道名（缺省回退到 `,` 后面的显示名）
- `tvg-logo`：台标直链，缺省时用 `CHANNEL_LOGO_PROVIDER` 模板拼
- `group-title`：频道分组，多个分组用 `;` 分隔
- 一行 `#EXTINF` 紧跟一行流地址

## 刷新

```bash
python3 scripts/refresh_sources.py
```

规则：

- 每个源少于 50 个频道视为异常，**保留旧文件**不覆盖
- iptv-org 的 cn/hk/mo/tw 任一失败，本轮不动国际源
- 写文件用先写 `.tmp` 再替换，避免半截文件
- GitHub Actions 每天 03:00（北京时间）跑一次，有变化自动提交
- 配置 `vars.SOURCES_DOMESTIC_BASE_URL` + OSS/COS Secrets 后，会额外同步一份到国内对象存储

## 版权

源地址均来自公开渠道，版权归各电视台/原站点。本仓库只做聚合、去重和格式统一，仅供个人学习研究。
若原站点要求下线，删除对应文件并重新跑一次刷新即可。
