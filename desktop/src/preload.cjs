const { contextBridge, ipcRenderer } = require("electron");

const UPDATE_STATE_CHANNEL = "shiyu:update:state";

contextBridge.exposeInMainWorld("shiyuDesktop", Object.freeze({
  isDesktop: true,
  updates: Object.freeze({
    getState: () => ipcRenderer.invoke("shiyu:update:get-state"),
    check: () => ipcRenderer.invoke("shiyu:update:check"),
    download: () => ipcRenderer.invoke("shiyu:update:download"),
    install: () => ipcRenderer.invoke("shiyu:update:install"),
    onState: callback => {
      if (typeof callback !== "function") return () => {};
      const listener = (_event, state) => callback(state);
      ipcRenderer.on(UPDATE_STATE_CHANNEL, listener);
      return () => ipcRenderer.removeListener(UPDATE_STATE_CHANNEL, listener);
    }
  })
}));
