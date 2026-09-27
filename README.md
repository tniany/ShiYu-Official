# 时遇

[![访问量](https://img.shields.io/endpoint?url=https%3A%2F%2Fshiy.tniay.top%2Fapi%2Fvisits%3Fformat%3Dshields)](https://shiy.tniay.top/)
[![今日访问](https://img.shields.io/endpoint?url=https%3A%2F%2Fshiy.tniay.top%2Fapi%2Fvisits%3Fformat%3Dshields%26metric%3Dtoday)](https://shiy.tniay.top/)

时遇是一款原生 Android 角色扮演智能体聊天应用。用户可以接入 OpenAI Chat Completions 兼容 API，导入角色卡，并为不同角色保存独立会话。

## 当前功能

- JSON 与 SillyTavern PNG 角色卡导入
- 角色列表、角色编辑、开场白与角色删除
- 多角色、多会话聊天，会话重命名与删除
- OpenAI 兼容 API 地址、API Key、模型配置
- 温度与上下文消息数配置
- 本地保存角色、会话、消息和设置
- 聊天错误提示、发送状态与可选择的消息文本
- 使用指定角色图片生成的多密度应用图标

所有角色和聊天数据仅存储在应用本机。API Key 会随请求直接发送到用户配置的接口，不经过其他服务。

## 构建

需要 JDK 17、Android SDK 33 和 Gradle 8.7 或兼容版本：

```powershell
.\gradlew.bat assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

由于仓库路径包含中文，项目已在 `gradle.properties` 中启用 Android Gradle Plugin 的路径检查覆盖选项。
