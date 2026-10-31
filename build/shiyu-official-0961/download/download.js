(() => {
  "use strict";
  window.lucide?.createIcons();
  const formatNumber = (value) => (Number(value) || 0).toLocaleString("zh-CN");
  const stats = document.querySelector("[data-download-stats]");
  if (stats) {
    fetch("/api/downloads", { headers: { "Cache-Control": "no-store" } })
      .then((response) => (response.ok ? response.json() : null))
      .then((data) => {
        if (!data || typeof data.total_downloads !== "number") return;
        stats.querySelector("[data-website-android-downloads]").textContent = formatNumber(data.website_android_downloads);
        stats.querySelector("[data-website-windows-downloads]").textContent = formatNumber(data.website_windows_downloads);
        stats.querySelector("[data-github-android-downloads]").textContent = formatNumber(data.github_android_downloads);
        stats.querySelector("[data-github-windows-downloads]").textContent = formatNumber(data.github_windows_downloads);
        stats.querySelector("[data-total-downloads]").textContent = formatNumber(data.total_downloads);
        stats.hidden = false;
      })
      .catch(() => {});
  }
  const button = document.getElementById("copy-checksum");
  const status = document.getElementById("copy-status");
  button?.addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText(document.getElementById("checksum-value").textContent.trim());
      status.textContent = "\u6821\u9a8c\u503c\u5df2\u590d\u5236";
    } catch {
      status.textContent = "\u590d\u5236\u5931\u8d25\uff0c\u53ef\u4e0b\u8f7d\u6821\u9a8c\u6587\u4ef6";
    }
  });
})();
