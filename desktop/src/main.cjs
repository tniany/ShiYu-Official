const { app, BrowserWindow, Menu, Tray, dialog, shell, session, screen, desktopCapturer, ipcMain, Notification } = require("electron");
const { autoUpdater } = require("electron-updater");
const fs = require("node:fs");
const path = require("node:path");
const {
  APP_ORIGIN, APP_ID, isAppPage, isLoginUrl, isAuthNavigation, isExternalUrl, safeBounds
} = require("./security.cjs");
const { createHandler } = require("./protocol.cjs");
const { fetchNetwork } = require("./network.cjs");

app.setName("Shiyu");
app.setAppUserModelId(APP_ID);
// Explicit paths keep development, installed and future upgraded builds stable.
app.setPath("userData", path.join(app.getPath("appData"), "Shiyu"));
if (process.env.SHIYU_TEST_USER_DATA && (process.env.SHIYU_TEST_MODE === "1")) {
  app.setPath("userData", path.resolve(process.env.SHIYU_TEST_USER_DATA));
}
const stateFile = path.join(app.getPath("userData"), "window-state.json");
const icon = path.join(__dirname, "../resources/icon.ico");
const webPreferences = {
  preload: path.join(__dirname, "preload.cjs"),
  nodeIntegration: false, contextIsolation: true, sandbox: true,
  webSecurity: true, allowRunningInsecureContent: false, webviewTag: false,
  backgroundThrottling: false, partition: "persist:shiyu"
};
let mainWindow;
let authWindow;
let tray;
let quitting = false;
let closeToTray = false;
let appSession;
const grantedPermissions = new Set();
const automaticPermissions = ["clipboard-sanitized-write", "fullscreen", "loopback-network", "display-capture"];

const UPDATE_STATE_CHANNEL = "shiyu:update:state";
const UPDATE_CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000;
const UPDATE_STARTUP_DELAY_MS = 8 * 1000;
let updateTimer;
let updateStartupTimer;
let updateCheckPromise;
let lastNotifiedUpdateVersion = "";
let updateState = {
  status: isUpdateEnabled() ? "idle" : "disabled", currentVersion: app.getVersion(), version: "", releaseName: "", releaseNotes: "",
  progress: null, message: isUpdateEnabled() ? "启动后会自动检查新版本。" : "开发版和测试版不检查在线更新。", checkedAt: 0, userInitiated: false
};

function showUpdateNotification(info) {
  const version = String(info?.version || "").trim();
  if (!version || version === lastNotifiedUpdateVersion || !Notification.isSupported()) return;
  lastNotifiedUpdateVersion = version;
  const notification = new Notification({
    title: "时遇有新版本",
    body: `时遇 ${version} 已可用，点击打开“关于与更新”下载。`,
    icon
  });
  notification.on("click", showMain);
  notification.show();
}

function readState() {
  try { return JSON.parse(fs.readFileSync(stateFile, "utf8")); } catch { return {}; }
}

function saveState() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  try {
    fs.mkdirSync(path.dirname(stateFile), { recursive: true });
    const temp = `${stateFile}.tmp`;
    fs.writeFileSync(temp, JSON.stringify({
      bounds: mainWindow.getNormalBounds(), maximized: mainWindow.isMaximized(), closeToTray
    }));
    fs.renameSync(temp, stateFile);
  } catch (error) { console.error("Could not save window state:", error.message); }
}

function releaseNotesText(value) {
  if (typeof value === "string") return value.trim().slice(0, 4000);
  if (Array.isArray(value)) return value.map(item => [item?.version, item?.note].filter(Boolean).join("\n")).filter(Boolean).join("\n\n").slice(0, 4000);
  return "";
}

function publicUpdateState() {
  return { ...updateState, currentVersion: app.getVersion(), progress: updateState.progress ? { ...updateState.progress } : null };
}

function setUpdateState(patch) {
  updateState = { ...updateState, ...patch, currentVersion: app.getVersion() };
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send(UPDATE_STATE_CHANNEL, publicUpdateState());
  return publicUpdateState();
}

function updateReleaseState(status, info, message) {
  return setUpdateState({
    status,
    version: String(info?.version || updateState.version || ""),
    releaseName: String(info?.releaseName || "").slice(0, 200),
    releaseNotes: releaseNotesText(info?.releaseNotes),
    message,
    checkedAt: Date.now()
  });
}

function isUpdateEnabled() {
  return app.isPackaged && process.env.SHIYU_TEST_MODE !== "1";
}

function isTrustedUpdateSender(event) {
  const url = event.senderFrame?.url || event.sender?.getURL?.() || "";
  return !!mainWindow && !mainWindow.isDestroyed() && event.sender === mainWindow.webContents && isAppPage(url);
}

async function checkForUpdates(userInitiated = false) {
  if (!isUpdateEnabled()) return setUpdateState({
    status: "disabled", message: "开发版和测试版不检查在线更新。", userInitiated, checkedAt: Date.now()
  });
  if (updateCheckPromise) { await updateCheckPromise; return publicUpdateState(); }
  setUpdateState({ status: "checking", message: "正在检查新版本…", userInitiated, checkedAt: Date.now() });
  updateCheckPromise = autoUpdater.checkForUpdates()
    .catch(error => setUpdateState({ status: "error", message: error?.message || "检查更新失败", userInitiated, checkedAt: Date.now() }))
    .finally(() => { updateCheckPromise = null; });
  await updateCheckPromise;
  return publicUpdateState();
}

async function downloadUpdate() {
  if (!isUpdateEnabled()) return setUpdateState({ status: "disabled", message: "开发版和测试版不下载在线更新。", userInitiated: true });
  if (["downloaded", "installing"].includes(updateState.status)) return publicUpdateState();
  if (!["available", "downloading"].includes(updateState.status)) {
    await checkForUpdates(true);
    if (updateState.status !== "available") return publicUpdateState();
  }
  if (updateState.status === "downloading") return publicUpdateState();
  setUpdateState({
    status: "downloading", message: "正在下载更新…", userInitiated: true,
    progress: { percent: 0, bytesPerSecond: 0, transferred: 0, total: 0 }
  });
  try { await autoUpdater.downloadUpdate(); }
  catch (error) { setUpdateState({ status: "error", message: error?.message || "下载更新失败", userInitiated: true, checkedAt: Date.now() }); }
  return publicUpdateState();
}

function installUpdate() {
  if (updateState.status !== "downloaded") return setUpdateState({ status: "error", message: "更新尚未下载完成", userInitiated: true });
  quitting = true;
  setUpdateState({ status: "installing", message: "正在重启并安装更新…", userInitiated: true });
  setImmediate(() => autoUpdater.quitAndInstall(false, true));
  return publicUpdateState();
}

function configureUpdater() {
  autoUpdater.autoDownload = false;
  autoUpdater.autoInstallOnAppQuit = true;
  autoUpdater.allowPrerelease = false;
  autoUpdater.allowDowngrade = false;
  autoUpdater.logger = console;
  autoUpdater.on("checking-for-update", () => setUpdateState({ status: "checking", message: "正在检查新版本…", checkedAt: Date.now() }));
  autoUpdater.on("update-available", info => {
    updateReleaseState("available", info, `发现新版本 ${info?.version || ""}，可以立即下载。`);
    showUpdateNotification(info);
  });

  autoUpdater.on("update-not-available", info => updateReleaseState("not-available", info, "当前已经是最新版本。"));
  autoUpdater.on("download-progress", progress => setUpdateState({
    status: "downloading", message: "正在下载更新…",
    progress: {
      percent: Math.max(0, Math.min(100, Number(progress?.percent || 0))),
      bytesPerSecond: Number(progress?.bytesPerSecond || 0),
      transferred: Number(progress?.transferred || 0),
      total: Number(progress?.total || 0)
    }
  }));
  autoUpdater.on("update-downloaded", info => updateReleaseState("downloaded", info, "更新已下载，重启时自动安装。"));
  autoUpdater.on("error", error => setUpdateState({ status: "error", message: error?.message || "更新服务暂不可用", checkedAt: Date.now() }));
}

function registerUpdateIpc() {
  ipcMain.handle("shiyu:update:get-state", event => isTrustedUpdateSender(event) ? publicUpdateState() : null);
  ipcMain.handle("shiyu:update:check", event => isTrustedUpdateSender(event) ? checkForUpdates(true) : null);
  ipcMain.handle("shiyu:update:download", event => isTrustedUpdateSender(event) ? downloadUpdate() : null);
  ipcMain.handle("shiyu:update:install", event => isTrustedUpdateSender(event) ? installUpdate() : null);
}

function startUpdateChecks() {
  if (!isUpdateEnabled() || updateTimer) return;
  updateStartupTimer = setTimeout(() => { updateStartupTimer = null; void checkForUpdates(false); }, UPDATE_STARTUP_DELAY_MS);
  updateStartupTimer.unref?.();
  updateTimer = setInterval(() => { void checkForUpdates(false); }, UPDATE_CHECK_INTERVAL_MS);
  updateTimer.unref?.();
}

function stopUpdateChecks() {
  if (updateStartupTimer) clearTimeout(updateStartupTimer);
  if (updateTimer) clearInterval(updateTimer);
  updateStartupTimer = null;
  updateTimer = null;
}

function showMain() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  if (mainWindow.isMinimized()) mainWindow.restore();
  mainWindow.show();
  mainWindow.focus();
}

async function openExternal(url) {
  if (isExternalUrl(url)) {
    try { await shell.openExternal(url); } catch { /* The browser may be unavailable. */ }
  }
}

function openLogin(url) {
  if (!isLoginUrl(url)) return;
  if (authWindow && !authWindow.isDestroyed()) { authWindow.focus(); return; }
  authWindow = new BrowserWindow({
    title: "\u65f6\u9047 - \u767b\u5f55", width: 560, height: 760,
    parent: mainWindow, autoHideMenuBar: true, icon, webPreferences: { ...webPreferences }
  });
  authWindow.setMenu(null);
  const opener = mainWindow.webContents;
  const contents = authWindow.webContents;
  const guard = (event, target) => {
    if (!isAuthNavigation(target)) { event.preventDefault(); void openExternal(target); }
  };
  contents.on("will-navigate", guard);
  contents.on("will-redirect", guard);
  contents.on("did-navigate", (_, target) => {
    if (isAppPage(target)) authWindow?.close();
  });
  contents.setWindowOpenHandler(({ url: target }) => {
    if (isAuthNavigation(target)) void authWindow?.loadURL(target).catch(() => {});
    else void openExternal(target);
    return { action: "deny" };
  });
  authWindow.on("closed", () => {
    authWindow = null;
    if (!quitting && mainWindow && !mainWindow.isDestroyed() && !opener.isDestroyed()) {
      opener.reload();
      showMain();
    }
  });
  void authWindow.loadURL(url).catch(() => {});
}

function buildMenu() {
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    { label: "\u65f6\u9047", submenu: [
      { label: "\u8fd4\u56de\u65f6\u9047", click: () => { showMain(); void mainWindow.loadURL(`${APP_ORIGIN}/`); } },
      { label: "\u6253\u5f00\u6570\u636e\u6587\u4ef6\u5939", click: () => { void shell.openPath(app.getPath("userData")); } },
      { type: "separator" },
      { label: "\u5173\u95ed\u7a97\u53e3\u65f6\u6700\u5c0f\u5316\u5230\u6258\u76d8", type: "checkbox", checked: closeToTray,
        click: item => { closeToTray = item.checked; saveState(); } },
      { label: "\u9000\u51fa", accelerator: "Alt+F4", click: () => app.quit() }
    ] },
    { label: "\u7f16\u8f91", submenu: [
      { label: "\u64a4\u9500", role: "undo" }, { label: "\u91cd\u505a", role: "redo" }, { type: "separator" },
      { label: "\u526a\u5207", role: "cut" }, { label: "\u590d\u5236", role: "copy" },
      { label: "\u7c98\u8d34", role: "paste" }, { label: "\u5168\u9009", role: "selectAll" }
    ] },
    { label: "\u89c6\u56fe", submenu: [
      { label: "\u91cd\u65b0\u52a0\u8f7d", role: "reload" },
      { label: "\u653e\u5927", role: "zoomIn" }, { label: "\u7f29\u5c0f", role: "zoomOut" },
      { label: "\u5b9e\u9645\u5927\u5c0f", role: "resetZoom" }, { label: "\u5168\u5c4f", role: "togglefullscreen" }
    ] },
    { label: "\u5e2e\u52a9", submenu: [
      { label: "\u68c0\u67e5\u66f4\u65b0", click: () => { showMain(); void checkForUpdates(true); } },
      { label: "\u65f6\u9047\u5b98\u7f51", click: () => { void openExternal("https://shiy.tniay.top"); } },
      { label: "\u5173\u4e8e\u65f6\u9047", click: () => {
        void dialog.showMessageBox(mainWindow, {
          type: "info", title: "\u5173\u4e8e\u65f6\u9047", message: `\u65f6\u9047 Windows ${app.getVersion()}`,
          detail: "\u804a\u5929\u3001\u89d2\u8272\u5361\u4e0e\u672c\u5730\u5907\u4efd\u3002\nWindows 10 / 11 (64-bit)\n\u57fa\u4e8e\u65f6\u9047 Web\u3002\u5b89\u5353\u4e13\u5c5e\u540e\u53f0\u670d\u52a1\u4e0e\u7cfb\u7edf\u60ac\u6d6e\u684c\u5ba0\u5c1a\u672a\u79fb\u690d\u3002"
        });
      } }
    ] }
  ]));
}

function configureSession() {
  appSession = session.fromPartition("persist:shiyu");
  const handler = createHandler(path.join(__dirname, "../renderer"), request => fetchNetwork(appSession, request));
  appSession.protocol.handle("https", handler);
  appSession.protocol.handle("http", handler);
  appSession.setPermissionCheckHandler((contents, permission, _origin, details) =>
    contents === mainWindow?.webContents && isAppPage(contents.getURL())
      && details.isMainFrame
      && (automaticPermissions.includes(permission) || grantedPermissions.has(permission)));
  appSession.setPermissionRequestHandler(async (contents, permission, callback, details) => {
    if (contents !== mainWindow?.webContents || !isAppPage(contents.getURL()) || !details.isMainFrame) return callback(false);
    if (automaticPermissions.includes(permission)) return callback(true);
    if (!["media", "notifications"].includes(permission)) return callback(false);
    const result = await dialog.showMessageBox(mainWindow, {
      type: "question", title: "\u65f6\u9047 - \u6743\u9650",
      message: permission === "media" ? "\u5141\u8bb8\u65f6\u9047\u4f7f\u7528\u9ea6\u514b\u98ce\u6216\u6444\u50cf\u5934\uff1f" : "\u5141\u8bb8\u65f6\u9047\u663e\u793a\u901a\u77e5\uff1f",
      buttons: ["\u4e0d\u5141\u8bb8", "\u5141\u8bb8"], defaultId: 0, cancelId: 0
    });
    if (result.response === 1) grantedPermissions.add(permission);
    callback(result.response === 1);
  });
  appSession.setDisplayMediaRequestHandler(async (request, callback) => {
    if (!mainWindow || request.frame !== mainWindow.webContents.mainFrame || !isAppPage(request.frame.url)) return callback({});
    try {
      const sources = await desktopCapturer.getSources({ types: ["screen"], thumbnailSize: { width: 0, height: 0 } });
      const result = await dialog.showMessageBox(mainWindow, {
        title: "\u9009\u62e9\u5171\u4eab\u5185\u5bb9", message: "\u8981\u5171\u4eab\u54ea\u4e2a\u5c4f\u5e55\uff1f",
        buttons: ["\u53d6\u6d88", ...sources.map(source => source.name)], defaultId: 0, cancelId: 0
      });
      callback(result.response > 0 ? { video: sources[result.response - 1] } : {});
    } catch { callback({}); }
  });
  appSession.on("will-download", (_, item) => {
    item.setSaveDialogOptions({ title: "\u4fdd\u5b58\u6587\u4ef6", defaultPath: path.join(app.getPath("downloads"), path.basename(item.getFilename())) });
    item.once("done", (_event, state) => {
      if (state === "interrupted" && mainWindow && !mainWindow.isDestroyed()) {
        void dialog.showMessageBox(mainWindow, { type: "error", message: "\u6587\u4ef6\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\u3002" });
      }
    });
  });
}

function createWindow() {
  const saved = readState();
  closeToTray = saved.closeToTray === true;
  mainWindow = new BrowserWindow({
    title: "\u65f6\u9047", ...safeBounds(saved.bounds, screen.getAllDisplays()),
    minWidth: 840, minHeight: 600, show: false, icon, backgroundColor: "#fdf6f8",
    webPreferences: { ...webPreferences }
  });
  mainWindow.webContents.on("will-attach-webview", event => event.preventDefault());
  const guard = (event, url) => {
    if (isAppPage(url)) return;
    event.preventDefault();
    if (isLoginUrl(url)) openLogin(url);
    else void openExternal(url);
  };
  mainWindow.webContents.on("will-navigate", guard);
  mainWindow.webContents.on("will-redirect", guard);
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (isLoginUrl(url)) openLogin(url); else void openExternal(url);
    return { action: "deny" };
  });
  mainWindow.webContents.on("render-process-gone", () => {
    if (!quitting) void dialog.showMessageBox(mainWindow, {
      type: "error", message: "\u754c\u9762\u8fd0\u884c\u4e2d\u65ad\uff0c\u53ef\u4ee5\u91cd\u65b0\u52a0\u8f7d\u3002", buttons: ["\u91cd\u65b0\u52a0\u8f7d", "\u9000\u51fa"]
    }).then(({ response }) => { if (response === 0) mainWindow.reload(); else app.quit(); });
  });
  mainWindow.once("ready-to-show", () => {
    if (saved.maximized) mainWindow.maximize();
    mainWindow.show();
  });
  mainWindow.on("close", event => {
    saveState();
    if (!quitting && closeToTray && tray) { event.preventDefault(); mainWindow.hide(); }
  });
  buildMenu();
  tray = new Tray(icon);
  tray.setToolTip("\u65f6\u9047");
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: "\u6253\u5f00\u65f6\u9047", click: showMain },
    { label: "\u9000\u51fa", click: () => app.quit() }
  ]));
  tray.on("double-click", showMain);
  void mainWindow.loadURL(`${APP_ORIGIN}/`).catch(error => {
    dialog.showErrorBox("\u65f6\u9047\u542f\u52a8\u5931\u8d25", error.message);
  });
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on("second-instance", showMain);
  app.on("before-quit", () => { quitting = true; stopUpdateChecks(); saveState(); });
  app.on("window-all-closed", () => app.quit());
  app.whenReady().then(() => { configureSession(); configureUpdater(); registerUpdateIpc(); createWindow(); startUpdateChecks(); }).catch(error => {
    dialog.showErrorBox("\u65f6\u9047\u542f\u52a8\u5931\u8d25", error.message);
    app.quit();
  });
}
