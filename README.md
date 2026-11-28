# 电视台

电视端直播应用 + 内置直播源，单仓库维护。

- 应用名：电视台
- 版本：1.4.12
- 包名：`com.doubao.tv`
- 内置源：央视/卫视/港澳台多线路包（92 频道 / 764 线路）+ 港澳台及国际（189 条线路）
- 源随 APK 打包，首次启动释放到应用私有目录，装完即可离线使用；联网后每 6 小时从仓库自动更新，优先走国内加速/可配置的国内 OSS，失败继续用本地副本
- 播放时只对当前频道/收藏做低速懒探测（3 并发、每条只读几十字节），自动换线优先跳可用线路，线路列表标注「可用/失效」
- 播放失败自动切下一条线路，整轮失败才停在错误页
- 打开即进直播（可在设置里关），界面为「复古奶油小电视」品牌视觉

## 目录结构

```
tv-live/
├── tv/            # 电视端 App（主模块，出 APK）
├── mobile/        # 手机端半成品，默认不参与构建（-PincludeMobile=true 显式打开）
├── core/          # data / util / designsystem / ui / settings
│   └── data/src/main/assets/sources/   # 打进 APK 的源（由脚本生成，勿手改）
├── feature/       # live：直播页、播放器、频道浮层与换线状态机
├── ijkplayer-java/
├── sources/
│   ├── dist/      # 源快照（进 git，可直接下载/在线订阅）
│   └── README.md  # 源清单与命名规则
├── scripts/
│   ├── crawl_sources.py     # 源发现爬虫：从专业源站找 m3u/txt
│   ├── crawl_check.py       # 线路巡检爬虫：测活 + HLS 清单/分片
│   ├── refresh_sources.py   # 国际源同步 + 逐条探测
│   ├── build_pack.py        # 多源合并 CLI（抓取/探测/选线/输出）
│   ├── sources_pipeline/    # 拆出的频道归一、解析与共享参数
│   ├── sign_sources.py      # 源清单 RSA 签名，构建前自动执行
│   └── upload_domestic.py   # 可选：同步到阿里云 OSS / 腾讯云 COS
├── docs/
│   └── SOURCES.md           # 接入点与维护说明
└── .github/workflows/
    ├── refresh-sources.yml  # 每天 03:00 发现源 + 合并 + 巡检
    ├── line-audit.yml       # 每周一 04:30 独立深巡检
    └── build-apk.yml        # 打 tag 出正式版（release 签名）
```

## 内置源

| 订阅名 | 规模 | 节目单 |
| --- | --- | --- |
| 内置·央视卫视（多线路） | 92 个频道 / 764 条线路（含港澳台、少儿分组） | m3u 自带 `x-tvg-url` |
| 内置·港澳台及国际 | 189 条线路（已剔除并入总包的频道） | m3u 自带 `x-tvg-url` |

每频道最多 12 条线路（同一 host 最多 2 条），按「实测可播 → IPv6 → 其他」排序；有活线时最多再带 2 条兜底，整台没有活线才留 4 条死链备用。固定 29 个入口（含 15 个非 GitHub 的国内源站），另有 `crawl_sources.py` 动态发现源参与合并。CCTV 频道显示「CCTV-1 综合」这类中文全名，少儿频道单独分组。节目单按频道名匹配，台标优先用 m3u 内的 `tvg-logo`，缺省回落到台标模板。

源评分和线路明细分别写到 `sources/dist/source-stats.json`、`sources/dist/line-stats.json`，每日任务会一起提交。

## 构建

```bash
# 云端正式版：打 tag 触发，构建成功后自动发布 Release
git tag v1.4.12 && git push origin v1.4.12

# 云端手动构建：在 Actions 里手动运行 build-apk，只留档 artifact、不发布 Release

# 本地（JDK 17 + Android SDK 36）
./gradlew :tv:assembleOriginalDebug
```

签名：仓库 Secrets 必须配 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`；缺失时构建直接失败，不再回退 debug 包。

## 源维护

```bash
python3 scripts/crawl_sources.py            # 源发现：找新 m3u/txt
python3 scripts/refresh_sources.py          # 国际源
python3 scripts/build_pack.py               # 多源合并 + 源/线路探测 + 评分
python3 scripts/crawl_check.py              # 线路巡检：测活 + HLS 深检
python3 scripts/refresh_sources.py --check  # 结构健康检查
python3 scripts/upload_domestic.py          # 可选：同步国内对象存储
```

拉取失败会保留旧文件并返回非 0，CI 不会把残缺源提交进来。

## 许可

- 本工程基于 MIT 协议的开源项目二次开发，`LICENSE` 保留原始版权声明（MIT 要求保留，未做删改）。
- 源数据版权归各电视台/原站点，仓库只做聚合、去重与格式整理，仅供个人学习研究，请勿商用或公开传播。
- 本项目不内置任何破解、解密能力，播放地址均来自公开渠道。
