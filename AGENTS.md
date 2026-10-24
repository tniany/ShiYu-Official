# Project site boundaries

- `https://shiy.tniay.top` is the public product website. Its Cloudflare Pages project is `shiyu-official`, and its deployable source is maintained under `build/shiyu-official-*`.
- `https://web-shiy.tniay.top` is the separate web application served by `square-worker` from `web/`.
- Never route `shiy.tniay.top` to `shiyu-character-square` and never deploy public-site design or release-page changes from `web/`.
- Website appearance, release announcements, and public download links requested for the official site must be implemented and deployed only through the `shiyu-official` Pages project.
