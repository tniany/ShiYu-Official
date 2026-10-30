const { net } = require("electron");
const { Readable } = require("node:stream");

function responseHeaders(values) {
  const headers = new Headers();
  for (const [key, value] of Object.entries(values)) {
    for (const item of Array.isArray(value) ? value : [value]) {
      if (item !== undefined) headers.append(key, String(item));
    }
  }
  return headers;
}

// Electron's fetch rejects manual redirects. ClientRequest exposes the redirect
// response so Chromium can navigate itself, retaining OAuth cookies and origin.
function fetchNetwork(session, request) {
  return new Promise((resolve, reject) => {
    const origin = request.headers.get("origin")
      || (request.referrer ? new URL(request.referrer).origin : new URL(request.url).origin);
    const headers = Object.fromEntries(request.headers);
    for (const name of ["host", "content-length", "connection", "transfer-encoding"]) delete headers[name];
    const client = net.request({
      url: request.url, method: request.method, session, headers, origin,
      credentials: request.credentials, redirect: "manual", bypassCustomProtocolHandlers: true
    });
    let settled = false;
    const finish = response => { settled = true; resolve(response); };
    const abort = () => {
      client.abort();
      if (!settled) reject(new DOMException("Request aborted", "AbortError"));
    };
    request.signal.addEventListener("abort", abort, { once: true });
    client.on("close", () => request.signal.removeEventListener("abort", abort));
    client.on("error", error => { if (!settled) reject(error); });
    client.on("redirect", (status, _method, location, values) => {
      const headers = responseHeaders(values);
      headers.set("location", location);
      finish(new Response(null, { status, headers }));
    });
    client.on("response", response => {
      const empty = request.method === "HEAD" || [204, 205, 304].includes(response.statusCode);
      finish(new Response(empty ? null : Readable.toWeb(response), {
        status: response.statusCode, statusText: response.statusMessage, headers: responseHeaders(response.headers)
      }));
      if (empty) response.resume();
    });
    if (request.signal.aborted) return abort();
    (async () => {
      if (request.body) {
        for await (const chunk of request.body) {
          await new Promise((done, fail) => client.write(Buffer.from(chunk), error => error ? fail(error) : done()));
        }
      }
      client.end();
    })().catch(error => { client.abort(); if (!settled) reject(error); });
  });
}

module.exports = { fetchNetwork };
