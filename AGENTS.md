# Project site boundaries

- `https://shiy.tniay.top` is the public product website. Its Cloudflare Pages project is `shiyu-official`, and its deployable source is maintained under `build/shiyu-official-*`.
- `https://web-shiy.tniay.top` is the separate web application served by `square-worker` from `web/`.
- Never route `shiy.tniay.top` to `shiyu-character-square` and never deploy public-site design or release-page changes from `web/`.
- Website appearance, release announcements, and public download links requested for the official site must be implemented and deployed only through the `shiyu-official` Pages project.

# Usage reporting compatibility contract

- `/api/open` on `https://shiy.tniay.top` is a production compatibility API for Android, Web, and login statistics. Do not remove, rename, redirect, or move it without an explicit user request and a migration plan for already-released clients.
- Every deployable `build/shiyu-official-*` source that contains `functions/api/open.js` must keep that file byte-for-byte identical to the canonical `build/shiyu-official-095/functions/api/open.js`. Before every official-site deployment, verify that the selected source contains the canonical Functions file.
- Keep accepting current requests and legacy JSON, form-encoded, GET-query, event-name, source, user-id, and version aliases. A reporting-format change must remain backward compatible with older clients.
- Do not change `OPEN_EVENT_URL`, `X-ShiYu-App`, the `open` / `web_visit` / `web_login` events, or the `/api/open` client call sites unless the user explicitly asks for a coordinated migration.
- Before pushing app, web, Worker, or official-site changes, run `node --test square-worker/test/usage-open.test.mjs`. Pull requests and pushes must also pass the usage contract workflow.
- After deploying the official site, verify `GET /api/open` returns `shiyu-open-counter`, `OPTIONS /api/open` returns 204 with the expected CORS headers, and a real Android open request reaches the database.
