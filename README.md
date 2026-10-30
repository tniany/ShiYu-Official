# 时遇

[![访问量](https://img.shields.io/endpoint?url=https%3A%2F%2Fshiy.tniay.top%2Fapi%2Fvisits%3Fformat%3Dshields)](https://shiy.tniay.top/)
[![今日访问](https://img.shields.io/endpoint?url=https%3A%2F%2Fshiy.tniay.top%2Fapi%2Fvisits%3Fformat%3Dshields%26metric%3Dtoday)](https://shiy.tniay.top/)

时遇是一款原生 Android 角色扮演智能体聊天应用。用户可以接入 OpenAI Chat Completions 兼容 API，导入角色卡，为不同角色保存独立会话，并通过内置工具、语音、桌宠和微信联动让角色融入日常生活。当前版本 **0.9.5**（versionCode 61）。

## 项目组成

| 目录 | 说明 |
|---|---|
| `app/` | Android 主应用（Kotlin + Java，Jetpack Compose 混合） |
| `web/` | Web 应用，由 `square-worker/`（Cloudflare Worker）在 `web-shiy.tniay.top` 提供 |
| `desktop/` | Windows 电脑版 Electron 客户端构建方案 |
| `build/shiyu-official-*` | 官网 `shiy.tniay.top` 的部署源（Cloudflare Pages 项目 `shiyu-official`） |
| `release/` | 版本说明、验证记录与历史发布资料（0.9.0 – 0.9.5） |

各目录的职责如下：

- `app/`：Android 原生客户端、后台服务、原生工具与本地推理能力。
- `web/`：Web 版应用源码，包含角色、聊天、朋友圈、备份、云同步与下载页面。
- `square-worker/`：`web-shiy.tniay.top` 的 Cloudflare Worker、API、D1 迁移与网络下载处理。
- `desktop/`：Windows 10 / 11 64 位 Electron 客户端，当前版本 `0.2.7`。
- `build/shiyu-official-095/`：当前官网 `shiy.tniay.top` 的 Cloudflare Pages 部署源。
- `release/`：版本说明、验证记录和历史发布资料；APK、安装包及分片等二进制产物不纳入主仓库。

## 当前功能

### 聊天与角色

- JSON、SillyTavern PNG 与 SHIY 角色卡导入、导出与系统分享
- 多角色、多会话聊天，会话重命名与删除，会话级对话模型设置
- 流式回复、发送状态、错误提示、生成重试与生成任务跟踪
- 表情包与 GIF 支持，消息分享与跨聊天内容转发，发送礼物
- 角色朋友圈：本地动态流，显式授权的角色可自主发布动态
- 默认角色卡「林晚星」

### 模型与生成

- OpenAI 兼容 API 地址、API Key、多供应商模型配置，温度与上下文消息数
- 本地推理引擎（llamatik）与本地模型下载管理，本地图像生成引擎
- 图片生成支持取消，视频生成支持独立供应商配置、任务查询下载并加入会话
- Live2D 模型支持，核心运行时按需下载

### 智能体工具

- 内置工具协议：角色可按需调用工具，敏感操作需用户逐次授权
- MCP 服务接入与推荐页，支持从 GitHub 导入 MCP 配置
- 墨迹天气、网易云音乐、必应搜索、网页元数据抓取等扩展信息源

### 语音与陪伴

- 语音对话：TTS / 语音识别，离线语音包按需下载
- 语音通话悬浮窗，一起听歌（会话内同步播放网易云音乐）
- 系统悬浮桌宠，带独立回复引擎，常驻通知保活与开机自启

### 微信联动

- 绑定授权的微信账号后，常驻服务监听新消息，角色可自动代收代复
- 密钥独立加密存储，可随时停用并撤销授权

### 数据与系统集成

- 角色、会话、消息、设置全部保存在应用本机
- 备份导出与导入，支持按内容勾选；历史数据损坏时自动进入数据抢救流程
- 崩溃保护与诊断日志，应用内更新检查与更新日志展示
- 中英双语界面，深浅色主题、气泡样式、字号、玻璃效果等外观自定义与配置迁移

## Windows 电脑版

Windows 10 / 11 64 位安装包和桌面端构建方案见 [desktop/README.md](desktop/README.md)。电脑版复用现有 Web 核心功能，内置运行环境，提供中文安装向导、快捷方式和卸载程序；不是安卓全部原生功能的完整移植。

## 在线服务

- `https://shiy.tniay.top` 为产品官网，由 Cloudflare Pages 项目 `shiyu-official` 部署，部署源维护在 `build/shiyu-official-*`。
- `https://web-shiy.tniay.top` 为 Web 应用，由 `square-worker` 从 `web/` 提供，同时承担 APK 下载分发。
- 应用内更新检查读取线上 `version.json`，并展示历史更新日志。
- 公共站点的改动只能通过 `shiyu-official` Pages 项目发布，不要从 `web/` 发布官网或发布页内容。

## 本地运行与构建

### Android

需要 JDK 17、Android SDK（compileSdk 36）和 Gradle 8.7 或兼容版本。项目使用 Android Gradle Plugin 8.6.1 与 Kotlin 1.9.24：

```powershell
.\gradlew.bat assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。由于仓库路径包含中文，`gradle.properties` 已启用 Android Gradle Plugin 的路径检查覆盖选项。

### Web

Web 版为无需构建的静态应用，可在 `web/` 目录启动本地服务器：

```powershell
cd web
.\start.ps1
```

默认地址为 `http://localhost:4173/`，端口被占用时可使用 `.\start.ps1 -Port 4174`。

### Cloudflare Worker

`square-worker/` 使用 Wrangler 运行和部署，提供 Web 静态资源、账号与广场 API、D1 数据库迁移和下载传输能力：

```powershell
cd square-worker
npx wrangler dev
npx wrangler deploy
```

本地登录状态、`.wrangler` 缓存和 `.auth-config` 不应提交到仓库。

### Windows 桌面端

需要 Node.js 22 或更新版本：

```powershell
cd desktop
npm ci
npm test
npm start
npm run dist
```

安装包输出到 `release/windows/`，不会修改 Web 源码或 Cloudflare 路由。

### 官网

官网部署源位于 `build/shiyu-official-095/`。预览或发布时必须使用 Cloudflare Pages 项目 `shiyu-official`：

```powershell
cd build/shiyu-official-095
npx wrangler pages dev .
npx wrangler pages deploy . --project-name shiyu-official
```

不要将 `shiy.tniay.top` 路由到 `shiyu-character-square`，也不要从 `web/` 部署官网或 Release 页面。

### 测试

```powershell
npm test --prefix desktop
node --test square-worker/test/windows-download.test.mjs
```

Android 代码可在仓库根目录运行 `gradlew.bat test`；发布前还应完成对应平台的安装、升级和回滚验证。

## 隐私

所有角色和聊天数据仅存储在应用本机。API Key 会随请求直接发送到用户配置的接口，不经过其他服务。角色广场、账号同步、更新检查等可选联网功能仅在用户主动启用时访问网络。
