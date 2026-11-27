# 接入说明（改了什么、怎么维护）

底座：MIT 协议的开源电视工程（Compose 版），版本 3.3.10。
本项目在它之上做的是「源 + 品牌 + 换线」三类改动，播放内核、EPG 渲染、UI 骨架保持原样，方便后续同步上游修复。

## 一、改动清单

| 文件 | 改动 | 原因 |
| --- | --- | --- |
| `settings.gradle.kts` | `:tbsx5` 改为目录存在才 include | 上游把 `../YYKM/tbsx5` 写死了，单仓独立构建会直接失败 |
| `core/data/.../utils/Constants.kt` | `IPTV_SOURCE_LIST` 换成两组内置源 + 一组在线源 | 上游默认是「无效占位订阅源」，装完没东西看 |
| 同上 | `EPG_SOURCE_LIST` 改用 GitHub 直链（raw + jsDelivr 备用） | fanmingming 的 .com/.cn 域名已失效，节目单空白 |
| `tv/.../utils/Configs.kt` | 经典选台界面默认开启；旧 EPG 域名自动迁移 | 左侧列表更好按；老用户 SP 里存的死链要就地修 |
| 同上 | `APP_TITLE`、`APP_REPO`、`GIT_RELEASE_LATEST_URL` | 品牌与本仓库地址 |
| 同上 | 移除 `GROUP_TELEGRAM` | 去掉外部联系方式 |
| `core/data/.../utils/BuiltinSources.kt` | 新增 | 首次启动把 assets 里的 m3u 释放到 `filesDir/sources/` |
| `tv/.../MyTVApplication.kt` | `AppData.init()` 后调用 `BuiltinSources.install(this)` | 保证释放时机在 Globals 初始化之后 |
| `tv/.../MainContentState.kt` | 新增 `handlePlayerError()` / `resetAutoSwitchLine()` | 播放失败自动换线 |
| `tv/.../MainContent.kt` | `LaunchedEffect` 监听播放错误触发换线 | 同上 |
| `AboutScreen.kt` | 删除赞赏、交流群入口 | 只用应用自身信息 |
| 全仓库 | 包名 `com.doubao.tv`、应用名「电视台」 | 品牌统一 |

## 二、源是怎么生效的

1. `BuiltinSources.install()` 把 `assets/sources/*.m3u` 复制到 `filesDir/sources/`。
2. `Constants.IPTV_SOURCE_LIST` 用 `Globals.fileDir` 拼出绝对路径，声明为 `isLocal = true`。
3. `IptvSource.needExternalStoragePermission()` 判断路径在私有目录内，所以**不需要存储权限**。
4. `IptvRepository` 对 `isLocal` 的源直接读文件、不走缓存；`M3uIptvParser` 解析后：
   - `tvg-name` → 节目单匹配名
   - `tvg-logo` → 台标（缺省回落到 `CHANNEL_LOGO_PROVIDER`）
   - `x-tvg-url` → 该源的默认节目单
5. 首次启动时 `Configs.iptvSourceCurrent` 默认取列表第一项，即「内置·央视卫视（多线路）」。

内置源更新后想让已安装的包也拿到，把 `BuiltinSources.SEEDED_VERSION` +1，下次启动会覆盖；
不加则老用户继续用旧文件，也可以通过「在线·自动更新」那组拿新源。

## 三、自动换线逻辑

`MainContentState.handlePlayerError()`：

- 只在同一频道内换线，`lineList` 按顺序轮换，最多轮一遍；
- 两次换线至少间隔 1 秒，避免错误回调连发时疯狂重试；
- 轮完仍未成功就停在错误页，交给用户手动换源或换台；
- 播放成功（error 变 null）或手动换台时计数清零。

## 四、升级上游

```bash
git remote add upstream <上游仓库地址>
git fetch upstream <分支>
git diff HEAD upstream/<分支> --stat     # 先看上游改了哪些
git merge upstream/<分支>                # 冲突大概率落在 Constants.kt
```

冲突处理原则：

- `Constants.kt`：保留我们的 `IPTV_SOURCE_LIST` / `EPG_SOURCE_LIST` / `APP_REPO`，其余跟上游。
- `settings.gradle.kts`：保留「目录存在才 include」的写法。
- `assets/sources/`：重新跑 `python3 scripts/build_pack.py` + `refresh_sources.py`。
- `MyTVApplication.kt`：保留 `BuiltinSources.install(this)`。

## 五、注意

- **IPv6 源在纯 IPv4 网络下全黑**：多线路包里 IPv6 排在后面，失败会自动跳过。
- **台标按频道名拼文件名**，频道名一改就会 404，优先让 m3u 自带 `tvg-logo`。
- **两套节目单不要混用**：国内源用中文频道名做 id，国际源用 iptv-org 的 guide。
- **打包第三方源有版权风险**：本仓库定位自用/学习，公开分发时建议改成「空壳 + 用户自己导入」。
- 上游 `mobile` 模块是半成品（没有自己的 Application，也没调 `AppData.init`），不要拿它出包。
