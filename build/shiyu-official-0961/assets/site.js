document.documentElement.classList.add("motion-ready");

const header = document.querySelector(".site-header");
const syncHeader = () => header?.classList.toggle("is-scrolled", window.scrollY > 24);
syncHeader();
window.addEventListener("scroll", syncHeader, { passive: true });

const revealTargets = document.querySelectorAll([
  ".experience-copy",
  ".phone-stage",
  ".story-row",
  ".capability-section .section-heading",
  ".capability-grid article",
  ".gallery-heading",
  ".gallery-shot",
  ".privacy-copy",
  ".privacy-screen",
  ".community-inner",
  ".download-inner",
  ".release-inner"
].join(","));

revealTargets.forEach((element) => element.classList.add("reveal"));

if ("IntersectionObserver" in window) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-visible");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.08, rootMargin: "0px 0px -36px" });
  revealTargets.forEach((element) => observer.observe(element));
} else {
  revealTargets.forEach((element) => element.classList.add("is-visible"));
}

const releaseList = document.querySelector("#release-list");

const compareVersions = (left, right) => {
  const parse = (value) => String(value || "0").replace(/^v/i, "").split("-")[0].split(".").map((part) => Number.parseInt(part, 10) || 0);
  const a = parse(left);
  const b = parse(right);
  for (let index = 0; index < Math.max(a.length, b.length); index += 1) {
    const difference = (a[index] || 0) - (b[index] || 0);
    if (difference !== 0) return difference;
  }
  return 0;
};

const renderReleaseHistory = (manifest) => {
  if (!releaseList) return;
  const current = manifest.version || "";
  const history = Array.isArray(manifest.release_history) ? manifest.release_history : [];
  releaseList.replaceChildren();
  if (!history.length) {
    const empty = document.createElement("p");
    empty.className = "release-loading";
    empty.textContent = "暂时没有版本记录。";
    releaseList.append(empty);
    return;
  }
  history.slice().sort((a, b) => compareVersions(b.version, a.version)).forEach((release) => {
    const item = document.createElement("article");
    item.className = "release-item";
    const comparison = compareVersions(release.version, current);
    if (comparison === 0) item.classList.add("is-current");
    const meta = document.createElement("div");
    meta.className = "release-meta";
    const version = document.createElement("strong");
    version.textContent = `时遇 ${release.version || "未知版本"}`;
    const date = document.createElement("time");
    date.textContent = release.published_at || release.publishedAt || "";
    meta.append(version, date);
    const body = document.createElement("div");
    body.className = "release-body";
    const status = document.createElement("span");
    status.className = "release-status";
    status.textContent = comparison > 0 ? "可更新" : comparison === 0 ? "当前版本" : "历史版本";
    body.append(status);
    const notes = document.createElement("ul");
    (Array.isArray(release.notes) ? release.notes : []).forEach((note) => {
      const entry = document.createElement("li");
      entry.textContent = note;
      notes.append(entry);
    });
    body.append(notes);
    item.append(meta, body);
    releaseList.append(item);
  });
};

if (releaseList) {
  fetch(`/version.json?history=${Date.now()}`, { cache: "no-store" })
    .then((response) => { if (!response.ok) throw new Error("release history unavailable"); return response.json(); })
    .then(renderReleaseHistory)
    .catch(() => { releaseList.querySelector(".release-loading")?.replaceChildren(document.createTextNode("版本记录暂时无法加载。")); });
}

const previewDialog = document.querySelector("#screenshot-dialog");
const previewImage = document.querySelector("#preview-image");
const previewCaption = document.querySelector("#preview-caption");

if (previewDialog && previewImage && previewCaption && typeof previewDialog.showModal === "function") {
  document.querySelectorAll("[data-preview]").forEach((link) => {
    link.addEventListener("click", (event) => {
      event.preventDefault();
      const sourceImage = link.querySelector("img");
      previewImage.src = link.href;
      previewImage.alt = sourceImage?.alt || "时遇应用界面";
      previewCaption.textContent = link.dataset.caption || previewImage.alt;
      previewDialog.showModal();
    });
  });

  document.querySelector("[data-close-preview]")?.addEventListener("click", () => previewDialog.close());
  previewDialog.addEventListener("click", (event) => {
    if (event.target === previewDialog) previewDialog.close();
  });
  previewDialog.addEventListener("close", () => {
    previewImage.src = "";
  });
}

// Visitor statistics
const visitorCounter = document.querySelector("[data-visits]");
const formatVisits = (value) => (Number(value) || 0).toLocaleString("zh-CN");
const paintVisits = (total) => {
  if (!visitorCounter) return;
  const label = visitorCounter.querySelector("strong");
  if (label) label.textContent = formatVisits(total);
  visitorCounter.hidden = false;
};
if (visitorCounter) {
  const SESSION_KEY = "shiyu_visit_counted";
  const alreadyCounted = (() => {
    try { return sessionStorage.getItem(SESSION_KEY) === "1"; } catch { return false; }
  })();
  fetch("/api/visits", alreadyCounted
    ? { method: "GET", headers: { "Cache-Control": "no-store" } }
    : { method: "POST", headers: { "Content-Type": "application/json" }, body: "" })
    .then((response) => (response.ok ? response.json() : null))
    .then((data) => {
      if (!data || typeof data.total !== "number") return;
      if (!alreadyCounted) {
        try { sessionStorage.setItem(SESSION_KEY, "1"); } catch {}
      }
      paintVisits(data.total);
    })
    .catch(() => {});
}

// GitHub community metadata
const githubStarFields = document.querySelectorAll("[data-github-stars]");
const formatStars = (value) => (Number(value) || 0).toLocaleString("zh-CN");
if (githubStarFields.length) {
  fetch("/api/github", { headers: { "Cache-Control": "no-cache" } })
    .then((response) => (response.ok ? response.json() : null))
    .then((data) => {
      if (!data || typeof data.stars !== "number") return;
      githubStarFields.forEach((field) => {
        field.textContent = formatStars(data.stars);
        field.title = `GitHub Star：${formatStars(data.stars)}`;
      });
    })
    .catch(() => {
      githubStarFields.forEach((field) => { field.title = "GitHub Star 数暂时不可用"; });
    });
}
