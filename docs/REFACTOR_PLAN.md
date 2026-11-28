# 电视台 / tv-live 架构评审与重构执行清单

> 基线：`main` / `7c41fde`（1.4.10）  
> 已推送 `main`；批次 1/2/3 的本地提交已同步到远端。  
> 目的：把已经确认的问题固定下来，按批次执行、逐项勾选，避免后续只做“看得见的功能”而漏掉结构性债。  
> 原则：不推倒重写；先封风险，再做可测试性，最后迁移 UI。每个批次都必须能单独编译、可回滚。

## 0. 当前架构快照

```text
MyTVApplication
├─ AppData / Globals / Configs
├─ BuiltinSources.install() + 6 小时在线更新
└─ 默认系统 TLS 校验（不再全局放行证书）

MainActivity
├─ HttpServerService（10481，仅开发者模式启动）
└─ Compose App
   └─ MainScreen / NavHost
      ├─ Loading → Dashboard / Live ...
      ├─ screen/*      新 UI：主页、频道、设置、搜索
      └─ screensold/*  旧 UI：直播页、播放器、频道列表

构建期
固定 29 个入口 + crawl_sources 动态发现 + refresh_sources 国际源
  → build_pack：归一 → 测活 → 选线 → HLS 深检 → pack + stats
  → assets/sources + sources/dist → tag → GitHub Actions → Release

运行期
APK assets → BuiltinSources → 本地 m3u → IptvRepository
  → MainViewModel → MainContentState → VideoPlayer（Media3 / IJK）
```

当前规模快照：

- `tv`：164 个 Kotlin 文件，约 2.83 万行。
- `screen`：67 个文件，约 1.08 万行；`screensold`：61 个文件，约 0.89 万行。直播页仍在 `screensold`。
- `Configs`：949 行；`SettingsViewModel`：700 行；`build_pack.py`：约 1040 行。
- 总源包：764 条 / 92 个频道名；存活 682；分片可拉取 488。
- 国际源：189 条；存活 77；当前快照仍有 112 条死链，代码已改为下次运行按频道剔除。
- 独立巡检：927 条；存活 740；分片可拉取 521。

## 1. 问题总表

状态：`TODO` 未开始 / `DOING` 进行中 / `DONE` 已完成并有验证 / `DEFERRED` 明确延后。

### P0 安全与完整性

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| SEC-01 | HTTP 配置服务默认启动、监听局域网、接口无鉴权 | `MainActivity.onCreate` 无条件 `HttpServer.startService`；`HttpServer` 监听 10481；无 auth 中间件 | Release 默认不启动；只有显式开发者开关才启动；服务可停止；至少补一次性配对/Token 方案 | DONE（默认关闭 + 开发者模式 + 随机 Token/HttpOnly Cookie；Token 持久化在独立 prefs） |
| SEC-02 | `/api/configs` 直接返回 `Configs.toPartial()`，含 Gist/Gitee Token、WebDAV 密码 | `HttpServer.handleConfigsGet` | 响应必须脱敏；任何日志、错误页、导出都不得包含密钥 | DONE |
| SEC-03 | 任意路径文件读写、APK/二进制上传安装接口 | `/api/file/content`、`/api/file/content-with-dir`、`/api/upload/apk`、`/api/upload/allinone` | 从 Release 路由中移除；如确有调试需要，只能在开发者模式且限制到专用目录/白名单 | DONE |
| SEC-04 | 全局信任所有 TLS 证书 | `UnsafeTrustManager.enableUnsafeTrustManager`；`OkHttpExtensions` 每个请求挂 `TrustAllSSLSocketFactory`；`TrustAllSSLSocketFactory` 初始化时改全局 `HttpsURLConnection` | 默认恢复系统证书校验；不再全局修改 `HttpsURLConnection`；自签名源改为显式、单源、可关闭的例外 | DONE（默认校验已恢复；单源例外未提供） |
| SEC-05 | 远端 `transformJs` 用 Rhino `initStandardObjects()` 执行，可触达 Java 类、无超时 | `IptvRepository.transform` | 使用安全标准对象 + ClassShutter 禁止 Java 访问 + 指令数超时；最终改为声明式转换 | DONE（安全对象/ClassShutter/指令上限已加；最终声明式转换待后续） |
| SEC-06 | 源 URL 缺少协议白名单；远端包只校验 `#EXTM3U` + 行数 | `M3uIptvParser` 接受任意非注释行；`BuiltinSources.looksValid` 只数 `#EXTINF` | App 解析器只接受明确的媒体协议；拒绝 `file://`、`javascript:` 等；远端包增加完整性校验 | DOING（协议白名单已做；包签名/哈希待补） |
| SEC-07 | 密钥明文存 SharedPreferences，`allowBackup` 可能带走 | `Configs` 的 token/password；Manifest | 迁移到 EncryptedSharedPreferences/Keystore；关闭或限制备份 | DOING（已关闭 allowBackup；密钥仍未加密） |

### P1 架构与状态

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| APP-01 | `Configs` / `SettingsViewModel` 是双份 god object，新增设置要改 4～5 处 | `Configs` 仍 949 行；`SettingsViewModel` 已从 700 行降到约 500 行；66 个字段仍手写 getter/setter | 单一 `SettingsRepository` + 不可变 `AppSettings`；设置读写只有一条路径 | DONE（UI 层已单入口；`Configs` 作为 SharedPreferences 兼容层保留，其他直接写 `Configs` 的调用点后续迁移） |
| APP-02 | 全局单例与组合期副作用 | `MainViewModel.instance`、`SettingsViewModel.instance`、`mainViewModel.needRefresh = {}` | 引入 `AppContainer`（不强制 Hilt）；ViewModel 通过构造注入；移除组合期赋值 | DOING（`AppContainer` 已提供 `settings`；`SettingsViewModel` 已构造注入；组合期赋值改事件流；`MainViewModel.instance` / `SettingsViewModel.instance` 仍在） |
| APP-03 | 新旧 UI 半迁移，直播页仍在 `screensold` | `screen` 67 文件 / `screensold` 61 文件；`Screens.Live` 走旧 `MainContent` | 抽出 `feature:live`；直播页、播放器、频道浮层迁移；旧包只留兼容壳 | TODO |
| APP-04 | 导航是字符串路由，返回逻辑分散在 App/MainScreen/MainContent | `Screens` 枚举 + route string；`BackHandler` 分散 | 类型安全 `Destination`；统一 `BackDispatcher`；启动页值校验 | TODO |
| APP-05 | `core:data` 依赖 Compose/Android 状态，领域层不纯 | `LineHealth` 用 `mutableStateMapOf`；实体用 `@Immutable` | 领域模型纯 Kotlin；健康状态改 `StateFlow`；UI 层只消费 | TODO |
| APP-06 | 播放失败自动换线有两条触发路径 | `VideoPlayerState.onError` 内自增；`MainContent` 又监听 `error` 调 `handlePlayerError`；EPG 回看排除逻辑只在一处 | 单一 `PlaybackCoordinator` 状态机；同一次错误只切换一次；回看/直播策略显式 | DOING（已收敛到 `handlePlayerError` 单入口 + `errorVersion`；独立 coordinator 未抽） |
| APP-07 | 每个请求新建 OkHttpClient，无连接池/ETag/统一超时 | `String.request` 每次 `OkHttpClient.Builder()` | 单例 OkHttpClient + 统一拦截器/超时/缓存策略 | DONE（已单例化 + 统一超时；ETag/缓存策略未做） |
| APP-08 | App 启动后全量探测 764 条线路，与 CI 巡检重复 | 原 `LineHealth.start` 探测全部线路；CI 已有 `line-stats` | 内置初始健康快照；只对当前频道/收藏懒探测 | DONE（已改当前频道 + 收藏懒探测；内置 `line-stats.json` 快照已加载） |

### P1 源流水线

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| SRC-01 | `build_pack.py` 单文件承担抓取/归一/探测/选线/输出，脚本互相 import | 957 行；`crawl_check` import `build_pack`；已有少量 unittest，未拆包 | 拆成 `sources_pipeline` 包；CLI 兼容；pytest 覆盖关键函数 | TODO |
| SRC-02 | 选线只看 `alive`，深检 `playable` 不参与选择 | `pick_lines` 排序键只有 `alive/cost`；总包 764 条中 488 条可拉 | 两阶段选线：深检后按 `playable → alive → latency → 源多样性 → host 上限` | DONE（代码已改：先扩池深检，再按 playable 定稿；待跑全量验证） |
| SRC-03 | 国际源死链全量写入 | `refresh_sources.probe_blocks` 只排序不剔除；189 条中 77 条存活 | 有活线时不再硬塞死链；每频道设置上限和兜底 | DONE（代码已改：按标准频道 ID 分组，活线优先，无活线才留 2 条兜底；待跑全量验证） |
| SRC-04 | `line-audit` 不回灌；03:00 与 04:30 重复巡检 | 产物只提交；两个 workflow 都跑 `crawl_check` | 巡检结果进入下一次选线；去掉重复任务或改成周度深检 | DONE（已回灌 route_score；每日重复巡检移除，独立任务改周度） |
| SRC-05 | 输出丢失 `http-user-agent` / `#KODIPROP` 等元数据 | `build_pack.parse` 只保留 name/logo/group/url；输出不写 KODIPROP | 能透明保留的元数据必须保留；不能支持的源要显式丢弃并记录原因 | DONE（已保留 EXTINF 的 UA、#EXTVLCOPT、#KODIPROP、#EXTGRP；待全量输出验证） |
| SRC-06 | `SEEDED_VERSION` 手工；更新失败也写 lastCheck | 旧版 `SEEDED_VERSION = 10`，1.4.9→1.4.10 源包 735→764 但版本未变；失败后 6 小时才重试；现已改哈希 | 用 assets 内容哈希判定是否重新释放；失败短退避，成功才记录 lastCheck | DOING（已改为 assets SHA-256 哈希释放；全部失败不写 lastCheck；仍未做内容清单/单文件版本） |
| SRC-07 | 远端包无签名/哈希；镜像可篡改 | `BuiltinSources.looksValid` 只校验开头和行数 | 包级签名或哈希校验；拒绝异常协议；镜像失败不影响旧包 | TODO |
| SRC-08 | 频道名/别名逻辑在 Python 与 Kotlin 各一份 | `build_pack.standard_name`、`hkmotw_ids`；`ChannelAlias`、`ChannelUtil` | 一份共享频道目录（JSON/YAML）生成两端资源；单一 ID/别名/排序来源 | TODO |

### P2 工程化与其它

| ID | 问题 | 证据 | 验收标准 | 状态 |
| --- | --- | --- | --- | --- |
| BLD-01 | README 版本/源数量过期 | README 写 1.4.8 / 735；实际 1.4.10 / 764 | README 由版本文件/脚本生成，或发版检查强制同步 | DONE（已同步 1.4.10 / 764 / 189；自动生成仍待做） |
| BLD-02 | README 说 push main 自动构建，workflow 只接受 tag/手动 | `build-apk.yml` | 文档与 workflow 一致；正式版只走 tag | DONE（已改为 tag 发布 / 手动只留档） |
| BLD-03 | `mobile` 半成品仍在默认构建 | `settings.gradle.kts` include `:mobile`；docs 明确不要出包 | 从默认构建移除，或改为显式 profile | DONE（默认不 include，-PincludeMobile=true 显式打开） |
| BLD-04 | 没有有效测试/静态检查 | `tv` 无 test；core 只有模板测试；scripts 无 pytest；CI 只编译 | CI 跑单测、lint、Python pytest；至少覆盖导航、解析、选线 | DOING（Python unittest 4 项 + core 单测已进 CI；tv 测试/lint 未做） |
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

- [ ] Token 轮换/一次性配对码；当前 Token 会出现在 URL query，可能进浏览器历史或代理日志。
- [ ] 自签名 HTTPS 源的单 host 例外（当前统一走系统信任，个别源会失败）。
- [ ] 远端源包的签名/哈希校验（当前只做协议白名单和最小行数）。
- [ ] SharedPreferences 密钥加密与备份策略（SEC-07）。

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
- [ ] `Configs` 仍是 66 字段的 SharedPreferences 兼容层；迁移其余直接读写 `Configs` 的调用点（云同步、HTTP、更新等）。
- [x] 单例 OkHttpClient、统一超时（ETag/缓存未做）。
- [x] 当前频道/收藏懒探测 + 内置 `line-stats.json` 初始快照。
- [x] 移除组合期 `needRefresh` 赋值，改事件流。

### 批次 4：直播 feature 化

- [ ] `:feature:live`。
- [ ] 类型安全导航 + 统一返回。
- [ ] `PlaybackCoordinator` 状态机。
- [ ] 旧 `screensold` 逐步退场。

## 3. 执行记录

| 日期 | 批次 | 改动 | 验证 | 备注 |
| --- | --- | --- | --- | --- |
| 2026-11-28 | 批次 1 | 建立本清单；HTTP 服务改为开发者模式启动并可 stop；随机 Token + HttpOnly Cookie 鉴权；移除任意文件读写/APK/二进制上传；configs 脱敏；移除全局 TrustAll；Rhino 改 safe objects + ClassShutter + 指令上限；m3u/txt 协议白名单；新增 StreamUrl 单测 | `:tv:compileOriginalReleaseKotlin`、`:core:data:testReleaseUnitTest` 通过 | 基线 `7c41fde`；Token 已加；自签名例外、包签名、密钥加密未做 |
| 2026-11-28 | 批次 2 | 选线扩池后按 playable 定稿；国际源按频道去死链；line-audit 分数回灌；assets 内容哈希释放 + 失败不写 lastCheck；保留 UA/KODIPROP；OkHttp 单例；README/workflow/mobile 收口；Python unittest + CI | `python3 -m unittest discover -s scripts` 4 项通过；`:tv:compileOriginalReleaseKotlin`、`:core:data:testReleaseUnitTest` 通过 | 未跑全量采集；SRC-01/07/08、SEC-07 加密仍未做 |
| 2026-11-28 | 批次 3（部分） | MainViewModel 的 `needRefresh` 组合期赋值改为 SharedFlow 事件；LineHealth 从全包扫描改为当前频道 + 收藏频道懒探测；打包 `line-stats.json` 并加载初始健康快照；播放错误换线收敛到 `handlePlayerError` 单入口；版本号收敛到 `version.properties` | `:tv:compileOriginalReleaseKotlin`、`:core:data:testReleaseUnitTest` 通过 | SettingsRepository/AppContainer 仍未做；APP-06 未抽独立 coordinator |
| 2026-11-28 | 批次 3（设置层） | 新增 `AppSettings`（66 字段不可变快照）与 `SettingsRepository`；`SettingsViewModel` 删除 66 份 `mutableStateOf` 与 `refresh()` 逐字段复制，构造注入仓库；新增 `AppContainer`；Compose 直接观察仓库 `current`；仓库只在字段变化时写 `Configs` | `:tv:compileOriginalReleaseKotlin` 通过；云端 run 46096707088 成功，APK 版本/签名校验通过 | `Configs` 仍是 SharedPreferences 兼容层；`SettingsViewModel` / `MainViewModel` 全局单例未移除 |

## 4. 构建验证记录

| 日期 | 提交 | 工作流 | 产物 | 说明 |
| --- | --- | --- | --- | --- |
| 2026-11-28 | `0e0f36f` | [run 46094249583](https://github.com/luanmuc/tv-live/actions/runs/46094249583) | `doubao-tv-1.4.10-refactor.apk`，32.5MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 `65:E9:...:10:CB` | 手动 `workflow_dispatch`，仅 artifact，未发 Release；CDN `https://aka.doubaocdn.com/s/I60hPHryAs` |
| 2026-11-28 | `64a4802` | [run 46096707088](https://github.com/luanmuc/tv-live/actions/runs/46096707088) | `doubao-tv-1.4.10.apk`，32.56MB，versionCode 15 / versionName 1.4.10，签名 SHA-256 与旧包一致 | 手动 `workflow_dispatch` 验证设置层重构；仅 artifact，未发 Release；另存 `doubao-tv-1.4.10-settings-refactor.apk`，CDN `https://aka.doubaocdn.com/s/jDTbMQaFk9` |

## 5. 风险与回滚

- 批次 1 会关闭 Release 的远程配置入口；这是有意的安全取舍。若仍需局域网配置，后续以“开发者模式 + 一次性配对码/Token”恢复，不用无鉴权服务。
- 恢复 TLS 校验后，个别自签名 HTTPS 源可能失败；应通过单源显式例外解决，不得恢复全局 TrustAll。
- 源流水线拆分期间保持 `scripts/build_pack.py` CLI 入口不变，避免 workflow 中断。
- 每次批次单独提交，保留旧 APK/源包可回滚；不在同一提交里混入 UI 大改。
