# Windows 0.2.7 下载发布

- 官网部署项目：Cloudflare Pages `shiyu-official`。
- 本次官网完整源：`build/shiyu-official-windows-026/`，以线上对应的 `build/shiyu-official-094-restored/` 为基线。
- 官网 Functions 源：`build/shiyu-official-windows-026/functions/`；部署必须从该官网源目录执行，否则 `/api/open` 不会随静态资源一起发布。
- 网页应用：`square-worker/wrangler.toml`，静态资源目录 `web/`。
- 下载页：两个域名的 `/download/`。规范地址为 `https://shiy.tniay.top/download/`。
- 主下载：`https://web-shiy.tniay.top/downloads/windows/Shiyu-0.2.7-win-x64-Setup.exe`。
- 自动更新清单：`https://web-shiy.tniay.top/downloads/windows/latest.yml`，同时提供 `.blockmap` 差分更新资源。
- 官网同名下载路径由官网 `_redirects` 跳转到主下载，未改变域名绑定。
- GitHub 备用发布：`tniany/shiyu-releases`，标签 `windows-v0.2.7`。不更改 Android 的 Latest 标记。
- 独立 Windows 版本清单：`/download/windows.json`。Android 的 `/version.json` 不变。

## 准备与验证

官网下载页面在 `build/shiyu-official-windows-026/download/` 中维护。脚本仅将这个自包含目录复制到 `web/download/`；不会用 `web/` 部署官网。

```powershell
node desktop/scripts/prepare-site-release.cjs
node --test square-worker/test/windows-download.test.mjs
node desktop/scripts/test-download-page.cjs
npx wrangler deploy --config square-worker/wrangler.toml --dry-run
```

准备脚本会核对安装包的 SHA-256，将其拆成 6 个不超过 20 MiB 的片段，并把 `latest.yml`、`.blockmap` 和分片存放到 `web/downloads/windows/`。下载接口逐段流式读取，支持 HEAD、Range、ETag、取消和读取失败，不把完整安装包存进 Worker 内存或浏览器 Service Worker 缓存。

## 分别部署

```powershell
npx wrangler deploy --config square-worker/wrangler.toml
npx wrangler --cwd build/shiyu-official-windows-026 pages deploy . --project-name shiyu-official --branch main --commit-dirty=true
```

发布后检查两个下载页、首页下载入口、Android 清单、安装包完整下载 SHA-256、跨分片 Range 请求、`latest.yml`、`.blockmap` 和 GitHub 备用附件。不要把 `shiy.tniay.top` 绑定到 `shiyu-character-square`。

安装包大小：114,524,161 字节（约 109.2 MiB）。

SHA-256：`2f2f2578167b994b32484e63153fa66b61e828c1c3c717fb8c84a45bca1eb0ba`。

下载页、`windows.json` 和校验文件中的对应值由 `prepare-site-release.cjs` 在构建后自动同步。
