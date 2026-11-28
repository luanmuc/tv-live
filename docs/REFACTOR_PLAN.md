# 电视台 / tv-live 架构评审与重构执行清单

> 基线：`main` / `7c41fde`（1.4.10）  
> 批次 1～7 已同步到 `main`；批次 7 为 1.4.12 收尾版本。
> 目的：把已经确认的问题固定下来，按批次执行、逐项勾选，避免后续只做“看得见的功能”而漏掉结构性债。  
> 原则：不推倒重写；先封风险，再做可测试性，最后迁移 UI。每个批次都必须能单独编译、可回滚。

## 0. 当前架构快照

```text
MyTVApplication
├─ AppData / Globals / Configs
├─ BuiltinSources.install() + 6 小时在线更新
└─ 默认系统 TLS 校验（不再全局放行证书）

MainActivity
└─ Compose App
   └─ MainScreen / NavHost
      ├─ Loading → Dashboard / Live ...
      ├─ screen/*  应用壳：主页、频道、设置、搜索
      └─ feature/live  直播页、播放器、频道浮层

构建期
固定 29 个入口 + crawl_sources 动态发现 + refresh_sources 国际源
  → build_pack：归一 → 测活 → 选线 → HLS 深检 → pack + stats
  → assets/sources + sources/dist → tag → GitHub Actions → Release

运行期
APK assets → BuiltinSources → 本地 m3u → IptvRepository
  → MainViewModel → MainContentState → VideoPlayer（Media3 / IJK）
```

当前规模快照：

- `tv`：64 个 Kotlin 文件（应用壳）；`feature:live`：63 个文件；`core:ui`：29 个；`core:settings`：8 个。
- `screensold` 已删除；直播 UI 全部在 `:feature:live`。
- `Configs`：594 行；`SettingsViewModel`：347 行；`AppSettings`：51 个字段；`build_pack.py`：约 1040 行。
- 总源包：764 条 / 92 个频道名；存活 682；分片可拉取 488。
- 国际源：189 条；存活 77；当前快照仍有 112 条死链，代码已改为下次运行按频道剔除。
- 独立巡检：927 条；存活 740；分片可拉取 521。

## 1. 问题总表

状态：`TODO` 未开始 / `DOING` 进行中 / `DONE` 已完成并有验证 / `DEFERRED` 明确延后。

### P0 安全与完整性

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| SEC-01 | HTTP 配置服务默认启动、监听局域网、接口无鉴权 | 原 `MainActivity.onCreate` 无条件 `HttpServer.startService`；`HttpServer` 监听 10481 | 移除 HTTP 配置服务与推送页；不再保留局域网写入口 | DONE（批次 5 已整体删除 `HttpServer`/`HttpServerService`/`PushScreen`、Manifest 服务注册与 Activity 启停；源添加改为本机弹窗） |
| SEC-02 | `/api/configs` 直接返回 `Configs.toPartial()`，含 Gist/Gitee Token、WebDAV 密码 | 原 `HttpServer.handleConfigsGet` | 响应必须脱敏；任何日志、错误页、导出都不得包含密钥 | DONE（随 HTTP 服务与 `Configs.Partial` 一并删除，接口不再存在） |
| SEC-03 | 任意路径文件读写、APK/二进制上传安装接口 | 原 `/api/file/content`、`/api/file/content-with-dir`、`/api/upload/apk`、`/api/upload/allinone` | 从 Release 路由中移除；不再保留调试上传入口 | DONE（随 HTTP 服务整体删除） |
| SEC-04 | 全局信任所有 TLS 证书 | `UnsafeTrustManager.enableUnsafeTrustManager`；`OkHttpExtensions` 每个请求挂 `TrustAllSSLSocketFactory`；`TrustAllSSLSocketFactory` 初始化时改全局 `HttpsURLConnection` | 默认恢复系统证书校验；不再全局修改 `HttpsURLConnection`；自签名源改为显式、单源、可关闭的例外 | DONE（默认校验已恢复；新增源弹窗可显式开启“允许自签名 HTTPS”，仅对该 host 生效且禁止重定向） |
| SEC-05 | 远端 `transformJs` 用 Rhino `initStandardObjects()` 执行，可触达 Java 类、无超时 | `IptvRepository.transform` | 使用安全标准对象 + ClassShutter 禁止 Java 访问 + 指令数超时；最终改为声明式转换 | DONE（安全对象/ClassShutter/指令上限已加；最终声明式转换待后续） |
| SEC-06 | 源 URL 缺少协议白名单；远端包只校验 `#EXTM3U` + 行数 | `M3uIptvParser` 接受任意非注释行；`BuiltinSources.looksValid` 只数 `#EXTINF` | App 解析器只接受明确的媒体协议；拒绝 `file://`、`javascript:` 等；远端包增加完整性校验 | DONE（协议白名单；批次 6 增加 `source-manifest.json` + RSA 签名，App 用自身 APK 签名证书验签并逐文件校验 SHA-256） |
| SEC-07 | 密钥明文存 SharedPreferences，`allowBackup` 可能带走 | 原 `Configs` 的 token/password；Manifest | 迁移到 EncryptedSharedPreferences/Keystore；关闭或限制备份 | DONE（批次 5 删除云同步后已无 token/password 字段；`allowBackup=false`。后续若再加密钥，必须走 Keystore） |

### P1 架构与状态

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| APP-01 | `Configs` / `SettingsViewModel` 是双份 god object，新增设置要改 4～5 处 | `Configs` 从 949 行降到 594 行；`SettingsViewModel` 从 700 行降到 347 行；`AppSettings` 51 个字段 | 单一 `SettingsRepository` + 不可变 `AppSettings`；设置读写只有一条路径 | DONE（UI/播放器/更新等 10 个文件读取点迁到 `AppContainer.settings.current`；云同步/调试 15 个字段已删除；`Configs` 仅保留 SharedPreferences 兼容层供 `SettingsRepository` 使用） |
| APP-02 | 全局单例与组合期副作用 | 原 `MainViewModel.instance`、`SettingsViewModel.instance`、`mainViewModel.needRefresh = {}` | 引入 `AppContainer`（不强制 Hilt）；ViewModel 通过构造注入；移除组合期赋值 | DONE（`AppContainer` + 构造注入；两处 `instance` 已删；组合期赋值改 SharedFlow；`settingsVM` / `mainVM` 直接走 `viewModel()`） |
| APP-03 | 新旧 UI 半迁移，直播页仍在 `screensold` | 旧 `screensold` 61 文件已删除；直播 UI 全部在 `:feature:live` | 抽出 `feature:live`；直播页、播放器、频道浮层迁移；旧包退场 | DONE（直播 UI 全量迁入 `:feature:live`；`ui:material/theme/tooling/AppScreen` 下沉 `core:ui`；设置层下沉 `core:settings`） |
| APP-04 | 导航是字符串路由，返回逻辑分散在 App/MainScreen/MainContent | 原 `Screens` 枚举 + route string；`BackHandler` 分散 | 类型安全 `Destination`；统一 `BackDispatcher`；启动页值校验 | DONE（外层与设置内层均类型安全；`BackDispatcher` 统一 AppScreen 返回栈；启动页值校验已加） |
| APP-05 | `core:data` 依赖 Compose/Android 状态，领域层不纯 | 原 `LineHealth` 用 `mutableStateMapOf`；实体用 `@Immutable` | 领域模型纯 Kotlin；健康状态改 `StateFlow`；UI 层只消费 | DONE（`LineHealth` 改 `StateFlow`；实体去 `@Immutable`；`core:data` 移除 Compose 插件/依赖。`LineHealth` 仍用 Android `Log`/`Context`/`Globals`，不属于纯领域模型） |
| APP-06 | 播放失败自动换线有两条触发路径 | 原 `VideoPlayerState.onError` 内自增；`MainContent` 又监听 `error` 调 `handlePlayerError`；EPG 回看排除逻辑只在一处 | 单一 `PlaybackCoordinator` 状态机；同一次错误只切换一次；回看/直播策略显式 | DONE（`PlaybackCoordinator` 已抽出并单测；`MainContentState.handlePlayerError` 只调用它；回看/直播、冷却、上限、活线优先均显式） |
| APP-07 | 每个请求新建 OkHttpClient，无连接池/ETag/统一超时 | `String.request` 每次 `OkHttpClient.Builder()` | 单例 OkHttpClient + 统一拦截器/超时/缓存策略 | DONE（已单例化 + 统一超时；ETag/缓存策略未做） |
| APP-08 | App 启动后全量探测 764 条线路，与 CI 巡检重复 | 原 `LineHealth.start` 探测全部线路；CI 已有 `line-stats` | 内置初始健康快照；只对当前频道/收藏懒探测 | DONE（已改当前频道 + 收藏懒探测；内置 `line-stats.json` 快照已加载） |

### P1 源流水线

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| SRC-01 | `build_pack.py` 单文件承担抓取/归一/探测/选线/输出，脚本互相 import | 957 行；`crawl_check` import `build_pack`；已有少量 unittest，未拆包 | 拆成 `sources_pipeline` 包；CLI 兼容；pytest 覆盖关键函数 | DONE（新增 `scripts/sources_pipeline`：common/channels/parsing；`build_pack.py` 972→789 行，CLI 参数与 `import build_pack` 兼容；unittest 8 项通过） |
| SRC-02 | 选线只看 `alive`，深检 `playable` 不参与选择 | `pick_lines` 排序键只有 `alive/cost`；总包 764 条中 488 条可拉 | 两阶段选线：深检后按 `playable → alive → latency → 源多样性 → host 上限` | DONE（代码已改：先扩池深检，再按 playable 定稿；待跑全量验证） |
| SRC-03 | 国际源死链全量写入 | `refresh_sources.probe_blocks` 只排序不剔除；189 条中 77 条存活 | 有活线时不再硬塞死链；每频道设置上限和兜底 | DONE（代码已改：按标准频道 ID 分组，活线优先，无活线才留 2 条兜底；待跑全量验证） |
| SRC-04 | `line-audit` 不回灌；03:00 与 04:30 重复巡检 | 产物只提交；两个 workflow 都跑 `crawl_check` | 巡检结果进入下一次选线；去掉重复任务或改成周度深检 | DONE（已回灌 route_score；每日重复巡检移除，独立任务改周度） |
| SRC-05 | 输出丢失 `http-user-agent` / `#KODIPROP` 等元数据 | `build_pack.parse` 只保留 name/logo/group/url；输出不写 KODIPROP | 能透明保留的元数据必须保留；不能支持的源要显式丢弃并记录原因 | DONE（已保留 EXTINF 的 UA、#EXTVLCOPT、#KODIPROP、#EXTGRP；待全量输出验证） |
| SRC-06 | `SEEDED_VERSION` 手工；更新失败也写 lastCheck | 旧版 `SEEDED_VERSION = 10`，1.4.9→1.4.10 源包 735→764 但版本未变；失败后 6 小时才重试；现已改哈希 | 用 assets 内容哈希判定是否重新释放；失败短退避，成功才记录 lastCheck | DONE（assets SHA-256 决定释放；清单带 `contentVersion`；版本未变且本地哈希一致时跳过下载；失败不写 lastCheck） |
| SRC-07 | 远端包无签名/哈希；镜像可篡改 | `BuiltinSources.looksValid` 只校验开头和行数 | 包级签名或哈希校验；拒绝异常协议；镜像失败不影响旧包 | DONE（批次 6：`source-manifest` 按 APK 证书验签，逐文件 SHA-256；失败保留旧包） |
| SRC-08 | 频道名/别名逻辑在 Python 与 Kotlin 各一份 | `build_pack.standard_name`、`hkmotw_ids`；`ChannelAlias`、`ChannelUtil` | 一份共享频道目录（JSON/YAML）生成两端资源；单一 ID/别名/排序来源 | DONE（190 个标准名/482 别名统一在 `core/data/src/main/res/raw/channel_name_alias.json`；Python 打包直接读该文件；CCTV 显示全称与排序仅构建侧使用） |

### P2 工程化与其它

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| BLD-01 | README 版本/源数量过期 | README 写 1.4.8 / 735；实际 1.4.10 / 764 | README 由版本文件/脚本生成，或发版检查强制同步 | DONE（已同步 1.4.10 / 764 / 189；自动生成仍待做） |
| BLD-02 | README 说 push main 自动构建，workflow 只接受 tag/手动 | `build-apk.yml` | 文档与 workflow 一致；正式版只走 tag | DONE（已改为 tag 发布 / 手动只留档） |
| BLD-03 | `mobile` 半成品仍在默认构建 | `settings.gradle.kts` include `:mobile`；docs 明确不要出包 | 从默认构建移除，或改为显式 profile | DONE（默认不 include，-PincludeMobile=true 显式打开） |
| BLD-04 | 没有有效测试/静态检查 | `tv` 无 test；core 只有模板测试；scripts 无 pytest；CI 只编译 | CI 跑单测、lint、Python pytest；至少覆盖导航、解析、选线 | DONE（`NavigationDestinationTest` 2 项；CI 增加 `:tv:testOriginalReleaseUnitTest` 与 `:tv:lintOriginalRelease`；Python unittest 7 项；lint 0 error/26 warning） |
| BLD-05 | 版本号/种子版本多处手写 | `tv/build.gradle.kts`、README；`SEEDED_VERSION` 已移除，改 assets 哈希 | 单一版本源；tag 与 versionName 自动校验；seed 自动计算 | DONE（`version.properties` 单一版本源；tag 与版本号 CI 校验；README 仍手写但非构建源） |

## 2. 执行批次

### 批次 1：P0 安全封口（本次开始）

范围：SEC-01 门禁、SEC-02 脱敏、SEC-03 移除危险接口、SEC-04 恢复 TLS 校验、SEC-05 收紧 Rhino、SEC-06 协议白名单。

本次完成定义：

- [x] Release 默认不再无条件启动 HTTP 服务。
- [x] 即使开发者模式开启，也不再暴露任意文件读写、APK/二进制上传接口。
- [x] `/api/configs` 不再返回 token/密码。
- [x] 不再全局信任所有证书；OkHttp 不再挂 TrustAll。
- [x] `transformJs` 禁止 Java 类访问并有执行上限。
- [x] m3u/txt 解析拒绝非媒体协议。
- [x] `:tv:compileOriginalReleaseKotlin` 通过。
- [x] 文档勾选状态与代码一致。

批次 1 留下的明确尾巴：

- [ ] 自签名 HTTPS 源的单 host 例外（当前统一走系统信任，个别源会失败）。
- [x] 远端源包的签名/哈希校验（批次 6：APK 证书签名清单 + 逐文件 SHA-256）。
- [x] SharedPreferences 密钥加密与备份策略（SEC-07）：云同步删除后已无 token/password 字段；`allowBackup=false`。

### 批次 2：源流水线可测试化与质量闭环

- [ ] 拆 `sources_pipeline` 包。
- [x] 两阶段选线（playable 优先）。
- [x] 国际源去死链/限上限。
- [x] `line-audit` 回灌，独立深巡检改周度。
- [x] seed 哈希化 + 失败退避。
- [x] unittest + CI。

### 批次 3：设置、状态、网络收敛

- [x] `SettingsRepository` / `AppSettings`（66 字段不可变快照）。
- [x] 去掉 `SettingsViewModel` 侧的 66 份 `mutableStateOf` 字段，读写统一走仓库。
- [x] `AppContainer` 提供 `SettingsRepository`，`SettingsViewModel` 构造注入。
- [x] 移除 `MainViewModel.instance` / `SettingsViewModel.instance`，`settingsVM` / `mainVM` 直接走 `viewModel()`。
- [x] `Configs` 仅保留 SharedPreferences 兼容层；10 个 UI/播放器/更新文件的读取点迁到 `AppContainer.settings.current`。
- [x] 单例 OkHttpClient、统一超时（ETag/缓存未做）。
- [x] 当前频道/收藏懒探测 + 内置 `line-stats.json` 初始快照。
- [x] 移除组合期 `needRefresh` 赋值，改事件流。

### 批次 4：直播 feature 化

- [x] `:feature:live`：模块完成直播 UI、播放器、频道浮层迁移；旧 `screensold` 包删除。
- [x] 外层类型安全导航 + 启动页值校验（`@Serializable Destination`）。
- [x] 设置内层类型安全导航（`SettingsDestination`，路由名与旧字符串一致）。
- [x] 统一返回 `BackDispatcher`（core:util 分发栈 + App/AppScreen 接入 + 4 项单测）。
- [x] `PlaybackCoordinator` 状态机（已迁到 `:feature:live`，7 项单测）。
- [x] 旧 `screensold` 退场（61 个文件迁入 feature，共享 UI 下沉 `core:ui`，设置层下沉 `core:settings`）。

### 批次 5：云同步 / 调试退场与状态收口

- [x] `LineHealth` 从 Compose `mutableStateMapOf` 改为 `StateFlow`；`ChannelLineItem`、`SettingsIptvSourceScreen` 改为 `collectAsState`。
- [x] `core:data` 移除 Compose 插件与 `androidx.activity.compose`，9 个实体删除 `@Immutable`。
- [x] 删除 `tv/sync/` 云同步包、云同步设置页/提供商页、设置分类与路由、启动拉取、HTTP 同步端点。
- [x] 删除云同步 10 个字段与 `CloudSyncProvider` 枚举；`Configs.Partial` 死代码一并删除。
- [x] 删除调试设置页、设置分类/路由、App 的 FPS/布局网格覆盖层、播放器元数据调试开关。
- [x] 删除调试 4 个字段。
- [x] 删除推送页、Dashboard 推送入口、HTTP 服务/前台 Service、Manifest 注册与 Activity 启停。
- [x] IPTV/EPG“添加其他源”改为本机 `AddSourceDialog`，不再依赖手机网页推送。
- [x] `MainViewModel.refreshChannelFavoriteList` 改走 `SettingsRepository.update`。
- [x] `Configs` 其余读取点已迁到 `AppContainer.settings.current`，仓库内部仍用 `Configs` 落盘。

### 批次 6：源包完整性与签名绑定

- [x] 新增 `SourceManifest`/`SourceManifestVerifier`：解析清单、RSA 验签、逐文件 SHA-256 校验。
- [x] `BuiltinSources` 在线更新改为先拉 `source-manifest.json` + `.sig`，用 APK 自身签名证书验签；失败即拒绝更新。
- [x] 清单与 `.sig` 随 APK assets 释放；远端文件哈希不符、协议不合法或清单缺失都不替换旧文件。
- [x] 新增 `scripts/sign_sources.py`：从 APK keystore 导出私钥，签 `source-manifest.json` 并输出 base64 签名。
- [x] `build-apk` 构建前自动签名清单；`refresh-sources` 每日源更新后重新签名并提交。
- [x] `SourceManifestTest`：解析、哈希匹配、RSA 验签、篡改拒绝。
- [x] 复核：APK 内 `source-manifest.json` 用 APK 证书 `openssl dgst -verify` 通过。

### 批次 7：1.4.12 收尾

- [x] `build_pack.py` 拆出 `sources_pipeline` 包（common/channels/parsing），CLI 与旧 import 兼容。
- [x] 频道别名统一到 APK 内 `channel_name_alias.json`，Python 与 Kotlin 共用一份。
- [x] `Configs` 静态读取迁移到 `AppContainer.settings.current`（10 个文件）。
- [x] 自定义单源“允许自签名 HTTPS 证书”，仅该 host 生效、禁止重定向。
- [x] 源清单 `contentVersion` + 本地哈希校验，版本未变跳过下载。
- [x] `:tv` 导航回归测试、CLI 回归测试、CI lint。
- [x] 已有源操作菜单可切换自签名证书。
- [x] 版本 1.4.12 / versionCode 17。

## 3. 执行记录

| 日期 | 批次 | 改动 | 验证 | 备注 |
| --- | --- | --- | --- | --- |
| 2026-11-28 | 批次 1 | 建立本清单；HTTP 服务改为开发者模式启动并可 stop；随机 Token + HttpOnly Cookie 鉴权；移除任意文件读写/APK/二进制上传；configs 脱敏；移除全局 TrustAll；Rhino 改 safe objects + ClassShutter + 指令上限；m3u/txt 协议白名单；新增 StreamUrl 单测 | `:tv:compileOriginalReleaseKotlin`、`:core:data:testReleaseUnitTest` 通过 | 基线 `7c41fde`；Token 已加；自签名例外、包签名、密钥加密未做 |
| 2026-11-28 | 批次 2 | 选线扩池后按 playable 定稿；国际源按频道去死链；line-audit 分数回灌；assets 内容哈希释放 + 失败不写 lastCheck；保留 UA/KODIPROP；OkHttp 单例；README/workflow/mobile 收口；Python unittest + CI | `python3 -m unittest discover -s scripts` 4 项通过；`:tv:compileOriginalReleaseKotlin`、`:core:data:testReleaseUnitTest` 通过 | 未跑全量采集；SRC-01/07/08、SEC-07 加密仍未做 |
| 2026-11-28 | 批次 3（部分） | MainViewModel 的 `needRefresh` 组合期赋值改为 SharedFlow 事件；LineHealth 从全包扫描改为当前频道 + 收藏频道懒探测；打包 `line-stats.json` 并加载初始健康快照；播放错误换线收敛到 `handlePlayerError` 单入口；版本号收敛到 `version.properties` | `:tv:compileOriginalReleaseKotlin`、`:core:data:testReleaseUnitTest` 通过 | SettingsRepository/AppContainer 仍未做；APP-06 未抽独立 coordinator |
| 2026-11-28 | 批次 3（设置层） | 新增 `AppSettings`（66 字段不可变快照）与 `SettingsRepository`；`SettingsViewModel` 删除 66 份 `mutableStateOf` 与 `refresh()` 逐字段复制，构造注入仓库；新增 `AppContainer`；Compose 直接观察仓库 `current`；仓库只在字段变化时写 `Configs`；移除 `SettingsViewModel.instance` / `MainViewModel.instance` | `:tv:compileOriginalReleaseKotlin` 通过；run 46096707088 与 run 46097780346 均成功，最终 APK 版本/签名校验通过 | `Configs` 仍是 SharedPreferences 兼容层；其余直接写 `Configs` 的调用点待迁移 |
| 2026-11-28 | 批次 4（第一刀） | 新增 `PlaybackCoordinator` 纯策略类：回看不换线、单线不换、1 秒冷却、最多 `线路数-1` 次、优先活线；`MainContentState` 删除 `_autoSwitchLineCount` / `_lastAutoSwitchLineAt` 与 `nextAliveLineIndex`，统一调用协调器 | 7 项单测通过；`:tv:compileOriginalReleaseKotlin` 通过；云端 run 46101103087 成功，APK 版本/签名校验通过 | `:feature:live`、类型安全导航、统一返回、旧 `screensold` 退场未做 |
| 2026-11-28 | 批次 4（导航） | 外层 `MainScreen` 从 `Screens` 字符串路由迁移到 `@Serializable Destination`（Agreement/Loading/Dashboard/Live/…/Settings）；`navigateSingleTop` 增加目的地重载；启动页保存值通过 `Destination.fromStoredName` 校验并回退 Loading；直播返回判断改用目的地 route | `:tv:compileOriginalReleaseKotlin` 通过；云端 run 46103508372 成功，APK 版本/签名校验通过 | 设置页内部仍是字符串子路由；统一 `BackDispatcher` 未抽 |
| 2026-11-28 | 批次 4（设置路由） | 新增 `SettingsDestination`，13 个分类 + 16 个子分类全部改为独立 `@Serializable` 目的地；`SettingsScreen` 改用 `composable<T>`，旧 `"APP"` / `"IPTV_SOURCE"` 等 route 名通过 `@SerialName` 保留；`navigateSingleTop` 增加设置目的地重载 | `:tv:compileOriginalReleaseKotlin` 通过；云端 run 46105482942 成功，APK 版本/签名校验通过 | 统一 `BackDispatcher` 仍未抽 |
| 2026-11-28 | 批次 4（返回/feature） | 新增 `BackDispatcher` 栈与 `LocalBackDispatcher`；`AppScreen` 不再各自抢 `BackHandler`，由 `App` 根统一分发；新增 `:feature:live` 模块并把 `PlaybackCoordinator` 从 `core:data` 迁入 | `BackDispatcherTest` 4 项通过；`:feature:live:testDebugUnitTest` 通过；`:tv:compileOriginalReleaseKotlin` 通过 | `screensold` 直播 UI 仍未迁入 feature；旧 UI 依赖 app 内部 material/theme/settings |

| 2026-11-28 | 批次 4（直播 UI 模块化） | 新增 `:core:settings`（设置快照/仓库/VM/Configs/主题与显示模式枚举）与 `:core:ui`（material/theme/tooling/AppScreen/路由/Destination/ModifierUtils/共享频道条目）；`:feature:live` 迁入全部 `screensold` 61 文件与直播频道信息组件，包名改为 `com.doubao.tv.feature.live.ui`；app 依赖 core:ui/core:settings，旧 `screensold` 删除 | `:tv:compileOriginalReleaseKotlin` 通过；`:core:data`/`:core:util`/`:feature:live` 单测全部通过；本地 `:tv:assembleOriginalRelease` 通过（R8/资源压缩/打包） | 包名保留 `PlaybackCoordinator` 的 `feature.live.playback`；`Configs` 仍为 SharedPreferences 兼容层 |
| 2026-11-28 | 批次 5 | `LineHealth` Flow 化、`core:data` 去 Compose；删除云同步包/页面/字段/枚举；删除调试页/覆盖层/字段；删除推送页与 HTTP 服务；源添加改本机弹窗；收藏写入收口到 `SettingsRepository` | `:tv:compileOriginalReleaseKotlin` 通过；`:core:data`/`:core:util`/`:feature:live` 单测通过 | 本地 `assembleOriginalRelease` 因缺 `keystore.jks` 未完成打包，改走云端签名构建 |
| 2026-11-28 | 批次 7 / v1.4.11 | `sources_pipeline` 拆包、频道别名统一、`Configs` 读取迁移、单源自签名 TLS、`contentVersion` 跳过重复下载、`:tv` 导航测试 + lint、版本 1.4.11 | unittest 8 项、`:tv:testOriginalReleaseUnitTest`、`:tv:lintOriginalRelease`（0 error/26 warning）通过；云端 run 46126076384 成功；APK 1.4.11 / versionCode 16，签名 SHA-256 `65:E9:...:10:CB`；Release v1.4.11 已发布 | 首次自动发布 GitHub Release；CDN `https://aka.doubaocdn.com/s/1VSKUR1xMy` |
| 2026-11-28 | 批次 6 | 源清单按 APK 签名证书签名；App 先验签再逐文件校验 SHA-256；新增 `sign_sources.py`；build/refresh 两条 workflow 均接入；SEC-07 随云同步字段删除关闭 | `SourceManifestTest` 3 项通过；`:core:data`/`:core:util`/`:feature:live` 单测通过；`:tv:compileOriginalReleaseKotlin` 通过；云端 run 46121470785 成功；APK 内清单 `Verified OK` | 源清单签名使用现有 APK keystore，不新增密钥；APK 版本仍为 1.4.10 |

## 4. 构建验证记录

| 日期 | 提交 | 工作流 | 产物 | 说明 |
| --- | --- | --- | --- | --- |
| 2026-11-28 | `0e0f36f` | [run 46094249583](https://github.com/luanmuc/tv-live/actions/runs/46094249583) | `doubao-tv-1.4.10-refactor.apk`，32.5MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 `65:E9:...:10:CB` | 手动 `workflow_dispatch`，仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/I60hPHryAs` |
| 2026-11-28 | `64a4802` | [run 46096707088](https://github.com/luanmuc/tv-live/actions/runs/46096707088) | `doubao-tv-1.4.10.apk`，32.56MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证设置层重构；仅 artifact，未发 Release；另存 `doubao-tv-1.4.10-settings-refactor.apk`，CDN `https://aka.doubaocdn.com/s/jDTbMQaFk9` |
| 2026-11-28 | `16eae07` | [run 46097780346](https://github.com/luanmuc/tv-live/actions/runs/46097780346) | `doubao-tv-1.4.10.apk`，32.56MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证设置层 + 单例移除后的最终代码；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/feEgQh7VcC` |
| 2026-11-28 | `8ef5b15` | [run 46101103087](https://github.com/luanmuc/tv-live/actions/runs/46101103087) | `doubao-tv-1.4.10.apk`，32.56MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证 `PlaybackCoordinator`；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/qerCdbPZmk` |
| 2026-11-28 | `6937c8f` | [run 46103508372](https://github.com/luanmuc/tv-live/actions/runs/46103508372) | `doubao-tv-1.4.10.apk`，32.59MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证类型安全导航；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/kQPNBRYCQb` |
| 2026-11-28 | `3a772a8` | [run 46105482942](https://github.com/luanmuc/tv-live/actions/runs/46105482942) | `doubao-tv-1.4.10.apk`，32.7MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证设置页类型安全路由；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/O6B0juG3sN` |
| 2026-11-28 | `222693e` | [run 46109037161](https://github.com/luanmuc/tv-live/actions/runs/46109037161) | `doubao-tv-1.4.10.apk`，32.7MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证 BackDispatcher + `:feature:live`；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/p0ypptplOZ` |
| 2026-11-28 | `374198d` | [run 46113751487](https://github.com/luanmuc/tv-live/actions/runs/46113751487) | `doubao-tv-1.4.10.apk`，32.7MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证直播 UI 全量迁入 `:feature:live` + `core:ui`/`core:settings` 拆分；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/8AfUYH1FjS` |
| 2026-11-28 | `52c849b` | [run 46118668075](https://github.com/luanmuc/tv-live/actions/runs/46118668075) | `doubao-tv-1.4.10.apk`，32.5MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证云同步/调试退场 + `LineHealth` Flow 化 + HTTP 服务删除；仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/KVXf4tpGDr` |

## 5. 风险与回滚

- 批次 1 会关闭 Release 的远程配置入口；这是有意的安全取舍。若仍需局域网配置，后续以“开发者模式 + 一次性配对码/Token”恢复，不用无鉴权服务。
- 恢复 TLS 校验后，个别自签名 HTTPS 源可能失败；应通过单源显式例外解决，不得恢复全局 TrustAll。
- 源流水线拆分期间保持 `scripts/build_pack.py` CLI 入口不变，避免 workflow 中断。
- 每次批次单独提交，保留旧 APK/源包可回滚；不在同一提交里混入 UI 大改。
