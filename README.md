# 豆包定制 TV

电视端直播应用 + 内置直播源，单仓库维护。

- 应用名：豆包定制
- 版本：1.1
- 包名：`com.doubao.tv`
- 内置源：央视/卫视多线路包（56 频道 / 260 线路）+ 港澳台及国际（216 频道）
- 源随 APK 打包，首次启动释放到应用私有目录，装完即可离线使用
- 播放失败自动切下一条线路，整轮失败才停在错误页

## 目录结构

```
tv-live/
├── tv/            # 电视端 App（主模块，出 APK）
├── mobile/        # 手机端工程
├── core/          # data / util / designsystem
│   └── data/src/main/assets/sources/   # 打进 APK 的源（由脚本生成，勿手改）
├── ijkplayer-java/
├── sources/
│   ├── dist/      # 源快照（进 git，可直接下载/在线订阅）
│   └── README.md  # 源清单与命名规则
├── scripts/
│   ├── refresh_sources.py   # 国际源同步
│   └── build_pack.py        # 央视/卫视多线路合并 + 存活探测
├── docs/
│   └── SOURCES.md           # 接入点与维护说明
└── .github/workflows/
    ├── refresh-sources.yml  # 每天 03:00（北京时间）刷新源
    └── build-apk.yml        # 构建 APK，push 更新 latest，打 tag 出正式版
```

## 内置源

| 订阅名 | 规模 | 节目单 |
| --- | --- | --- |
| 内置·央视卫视（多线路） | 56 个频道 / 260 条线路 | m3u 自带 `x-tvg-url` |
| 内置·港澳台及国际 | 216 个频道 | m3u 自带 `x-tvg-url` |

每个频道最多 6 条线路，按「实测可播 → 响应快 → 其他」排序。节目单按频道名匹配，台标优先用 m3u 内的 `tvg-logo`，缺省回落到台标模板。

## 构建

```bash
# 云端：push 到 main 自动构建，产物更新到 latest Release
# 正式版：打 tag 触发
git tag v1.1 && git push origin v1.1

# 本地（JDK 17 + Android SDK 36）
./gradlew :tv:assembleOriginalDebug
```

签名：在仓库 Secrets 配 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 即可出 release 包，否则产出 debug 包。

## 源维护

```bash
python3 scripts/refresh_sources.py          # 国际源
python3 scripts/build_pack.py               # 央视/卫视多线路包
python3 scripts/refresh_sources.py --check  # 健康检查
```

拉取失败会保留旧文件并返回非 0，CI 不会把残缺源提交进来。

## 许可

- 本工程基于 MIT 协议的开源项目二次开发，`LICENSE` 保留原始版权声明（MIT 要求保留，未做删改）。
- 源数据版权归各电视台/原站点，仓库只做聚合、去重与格式整理，仅供个人学习研究，请勿商用或公开传播。
- 本项目不内置任何破解、解密能力，播放地址均来自公开渠道。
