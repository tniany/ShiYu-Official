(() => {
  "use strict";
  window.lucide?.createIcons();
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
