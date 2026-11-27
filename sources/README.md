# sources

内置直播源的唯一维护位置。

```
sources/dist/                       # 源快照，进 git
├── pack-cctv-satellite.m3u         # 央视 + 卫视多线路包（56 频道 / 260 线路）
└── intl-hkmo-tw.m3u                # 港澳台及国际（216 个）

core/data/src/main/assets/sources/  # 打包进 APK 的副本（脚本自动同步，勿手改）
```

## 来源

| 文件 | 生成方式 | 说明 |
| --- | --- | --- |
| `pack-cctv-satellite.m3u` | `scripts/build_pack.py` | 合并 fanmingming index/itv/ipv6 + iptv-org cn，按频道名归一后每频道保留最多 6 条线路，实测可播的排前面 |
| `intl-hkmo-tw.m3u` | `scripts/refresh_sources.py` | iptv-org `countries/{cn,hk,mo,tw}.m3u` 合并去重 |

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

## 版权

源地址均来自公开渠道，版权归各电视台/原站点。本仓库只做聚合、去重和格式统一，仅供个人学习研究。
若原站点要求下线，删除对应文件并重新跑一次刷新即可。
