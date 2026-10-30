# 时遇

[![访问量](https://img.shields.io/endpoint?url=https%3A%2F%2Fshiy.tniay.top%2Fapi%2Fvisits%3Fformat%3Dshields)](https://shiy.tniay.top/)
[![今日访问](https://img.shields.io/endpoint?url=https%3A%2F%2Fshiy.tniay.top%2Fapi%2Fvisits%3Fformat%3Dshields%26metric%3Dtoday)](https://shiy.tniay.top/)

时遇是一款面向 Android、Web 和 Windows 的角色扮演智能体聊天应用。你可以连接 OpenAI Chat Completions 兼容接口，导入角色卡，与角色聊天、分享动态，并使用语音、桌宠、音乐和扩展工具等功能。

![时遇产品展示](build/shiyu-official-095/assets/hero-dfddcde54819456783eea1c62289486a.png)

当前 Android 版本为 **0.9.5**，Windows 版本为 **0.2.7**。

## 产品入口

- [产品官网](https://shiy.tniay.top/)
- [Web 版](https://web-shiy.tniay.top/)
- [Android 发布记录](https://github.com/tniany/shiyu-releases/releases)

## 项目组成

| 目录 | 说明 |
|---|---|
| `app/` | Android 主应用（Kotlin + Java，Jetpack Compose 混合） |
| `web/` | Web 版应用源码与下载页面 |
| `square-worker/` | Cloudflare Worker API、数据服务与下载传输 |
| `desktop/` | Windows Electron 客户端源码 |
| `build/shiyu-official-095/` | 当前官网部署源 |
| `release/` | 版本说明与历史发布资料 |

APK、安装包及分片等二进制文件不在主仓库中长期保存。

## 功能亮点

- 导入 JSON、SillyTavern PNG 和 SHIY 角色卡，管理多角色、多会话聊天
- 支持流式回复、多模型配置、图片与视频生成，以及可选的本地模型能力
- 角色可以发布朋友圈，支持评论、点赞和权限控制
- 内置天气、音乐、搜索、网页解析和 MCP 等扩展工具
- 支持 TTS、语音识别、语音通话、一起听歌和桌面宠物
- 支持微信联动，角色可在授权后接收并回复消息
- 提供数据备份、恢复、诊断和跨平台内容同步能力
- 提供 Android、Web 和 Windows 三种客户端形态

## Windows 电脑版

Windows 10 / 11 64 位安装包和桌面端构建方案见 [desktop/README.md](desktop/README.md)。电脑版复用现有 Web 核心功能，内置运行环境，提供中文安装向导、快捷方式和卸载程序；不是安卓全部原生功能的完整移植。

## 本地开发

### Android

需要 JDK 17、Android SDK（compileSdk 36）以及兼容的 Gradle 环境：

```powershell
.\gradlew.bat assembleDebug
```

### Web

在 `web/` 目录启动本地静态服务器：

```powershell
cd web
.\start.ps1
```

默认访问 `http://localhost:4173/`。

### Windows 桌面端

需要 Node.js 22 或更新版本：

```powershell
cd desktop
npm ci
npm test
npm start
```

### 常用测试

```powershell
npm test --prefix desktop
node --test square-worker/test/windows-download.test.mjs
```

## 参与开发

- Android 代码位于 `app/`
- Web 页面与交互位于 `web/`
- 服务端接口与迁移位于 `square-worker/`
- Windows 客户端位于 `desktop/`
- 官网内容位于 `build/shiyu-official-095/`

提交代码前请确认相关平台可以正常构建，并运行对应测试。

## 使用组件

- [萌萌计数器](https://github.com/journey-ad/Moe-counter)：用于网站访问计数展示。
