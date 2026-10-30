const CACHE_NAME = "shiyu-web-1.4.15-windows026";
const CORE_ASSETS = [
  "./",
  "./index.html",
  "./styles.css?v=1.4.15",
  "./app.js?v=1.4.15",
  "./manifest.webmanifest",
  "./assets/character.png",
  "./assets/icon-192.png",
  "./assets/icon-512.png"
];

self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(CORE_ASSETS)));
  self.skipWaiting();
});

self.addEventListener("activate", event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key)))));
  self.clients.claim();
});

self.addEventListener("fetch", event => {
  if (event.request.method !== "GET") return;
  const url = new URL(event.request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith("/auth/") || url.pathname.startsWith("/api/") || url.pathname.startsWith("/manage-") || url.pathname.startsWith("/download")) return;

  // 导航请求（HTML）走「网络优先」：HTML 不带版本号，一旦被缓存住，
  // 回访用户会一直拿到旧 HTML，从而一直指向旧 CSS，样式看起来就是坏的。
  if (event.request.mode === "navigate") {
    event.respondWith(fetch(event.request).then(response => {
      if (response.ok) {
        const copy = response.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy));
      }
      return response;
    }).catch(() => caches.match(event.request).then(cached => cached || caches.match("./index.html"))));
    return;
  }

  // 其余静态资源保持「缓存优先」，它们都带 ?v= 版本号，改版即换 URL。
  event.respondWith(caches.match(event.request).then(cached => {
    const network = fetch(event.request).then(response => {
      if (response.ok) caches.open(CACHE_NAME).then(cache => cache.put(event.request, response.clone()));
      return response;
    });
    return cached || network.catch(() => caches.match("./index.html"));
  }));
});
