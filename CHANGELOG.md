# 1.2

## 直播源

- 港澳台 27 个中文频道并入央视/卫视包：新增「港澳台」分组
  （翡翠台、凤凰中文/资讯、ViuTV、天映、美亚、澳视 4 台、莲花、台视、华视、中天、民视、东森、TVBS、公视、客家、原民、大爱、好消息）
- 购物台（BSTV、MOMO TV）与 11 条 Geo-blocked 频道不入包
- 包体变为 83 频道 / 287 线路

## 工程

- CI 只保留 tag 触发，构建缓存（SDK + Gradle）全开，单次构建更快
- 版本 1.2（versionCode 3），内置源种子版本 4

# 1.1

## 应用

- 选台界面默认使用「经典三段式」：左栏分组+频道、右侧节目单
- 只发正式版：CI 固定构建 release 并签名，不再产出／发布 debug 包
- 移除 Sentry 依赖（DSN 一直为空，纯占体积且带 16KB 不友好的 native 库）

## 节目单

- 修复节目单空白：fanmingming 的 `live.fanmingming.com` / `.cn` 已失效，
  改走 `raw.githubusercontent.com/fanmingming/live/main/e.xml`，jsDelivr 作备用
- 同步修复内置源内的台标地址与 m3u 头 `x-tvg-url`

## 工程

- 版本 1.1（versionCode 2）
- 内置源种子版本 3，覆盖旧文件

# 1.0

首个自建版本，基于 MIT 协议的上游工程重构。

## 应用

- 应用名统一为「豆包定制」，包名 `com.doubao.tv`
- 版本号从 1.0 重新起算，版本历史清零
- 移除赞赏入口、交流群入口、第三方作者信息与外部更新通道
- 清除「马甲包」构建风味，只保留 original
- 检查更新指向本仓库 Release

## 直播源

- 内置央视/卫视多线路包：56 个频道 / 260 条线路
- 内置港澳台及国际源：216 个频道
- 新增播放失败自动换线：同频道依次尝试下一条线路
- 新增 `scripts/build_pack.py`：多上游合并、频道名归一、存活探测、多线路排序

## 工程

- 单仓库同时维护软件与源
- GitHub Actions：每天刷新源；push 更新 latest Release；打 tag 出正式版
