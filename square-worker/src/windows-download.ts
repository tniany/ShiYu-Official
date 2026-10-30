export const WINDOWS_RELEASE = {
  version: "0.2.7",
  filename: "Shiyu-0.2.7-win-x64-Setup.exe",
  size: 114524161,
  partSize: 20 * 1024 * 1024,
  sha256: "2f2f2578167b994b32484e63153fa66b61e828c1c3c717fb8c84a45bca1eb0ba",
};
type Release = typeof WINDOWS_RELEASE;
type AssetBinding = { fetch(request: Request): Promise<Response> };

export function parseRange(value: string | null, size: number): { start: number; end: number } | null {
  if (!value) return { start: 0, end: size - 1 };
  const match = /^bytes=(\d*)-(\d*)$/.exec(value);
  if (!match || (!match[1] && !match[2])) return null;
  if (!match[1]) {
    const length = Number(match[2]);
    return Number.isSafeInteger(length) && length > 0 ? { start: Math.max(0, size - length), end: size - 1 } : null;
  }
  const start = Number(match[1]);
  const end = match[2] ? Number(match[2]) : size - 1;
  if (!Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start >= size || start > end) return null;
  return { start, end: Math.min(end, size - 1) };
}

export async function streamWindows(request: Request, assets: AssetBinding, release: Release = WINDOWS_RELEASE): Promise<Response> {
  const etag = `"${release.sha256}"`;
  const headers = new Headers({
    "content-type": "application/vnd.microsoft.portable-executable",
    "content-disposition": `attachment; filename="${release.filename}"`,
    "accept-ranges": "bytes",
    "etag": etag,
    "cache-control": "public, max-age=86400, immutable, no-transform",
    "x-content-type-options": "nosniff",
  });
  if (request.headers.get("if-none-match")?.split(",").map(value => value.trim()).includes(etag)) {
    return new Response(null, { status: 304, headers });
  }
  const ifRange = request.headers.get("if-range");
  const rangeHeader = !ifRange || ifRange === etag ? request.headers.get("range") : null;
  const range = parseRange(rangeHeader, release.size);
  if (!range) {
    headers.set("content-range", `bytes */${release.size}`);
    return new Response(null, { status: 416, headers });
  }
  const count = range.end - range.start + 1;
  headers.set("content-length", String(count));
  if (rangeHeader) headers.set("content-range", `bytes ${range.start}-${range.end}/${release.size}`);
  const status = rangeHeader ? 206 : 200;
  if (request.method === "HEAD") return new Response(null, { status, headers });

  // Read only the requested portions, with backpressure and no whole-file buffering.
  let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;
  let cancelled = false;
  async function* chunks() {
    const first = Math.floor(range!.start / release.partSize);
    const last = Math.floor(range!.end / release.partSize);
    for (let part = first; part <= last && !cancelled; part++) {
      const partLength = Math.min(release.partSize, release.size - part * release.partSize);
      const start = part === first ? range!.start % release.partSize : 0;
      const end = part === last ? range!.end % release.partSize : partLength - 1;
      const url = new URL(`/downloads/windows/${release.filename}.part${String(part + 1).padStart(2, "0")}`, request.url);
      const response = await assets.fetch(new Request(url, { headers: { Range: `bytes=${start}-${end}` } }));
      if (cancelled) { await response.body?.cancel(); return; }
      if (![200, 206].includes(response.status) || !response.body) throw new Error(`Windows download part ${part + 1} unavailable`);
      let skip = response.status === 206 ? 0 : start;
      let remaining = end - start + 1;
      reader = response.body.getReader();
      try {
        while (remaining > 0 && !cancelled) {
          const { value, done } = await reader.read();
          if (done) { if (cancelled) return; throw new Error("Incomplete Windows download part"); }
          if (skip >= value.length) { skip -= value.length; continue; }
          const output = value.subarray(skip, Math.min(value.length, skip + remaining));
          skip = 0;
          remaining -= output.length;
          yield output;
        }
      } finally {
        await reader.cancel().catch(() => {});
        reader.releaseLock();
        reader = undefined;
      }
    }
  }
  const iterator = chunks();
  return new Response(new ReadableStream<Uint8Array>({
    async pull(controller) {
      try {
        const result = await iterator.next();
        if (cancelled) return;
        if (result.done) controller.close(); else controller.enqueue(result.value);
      } catch (error) { if (!cancelled) controller.error(error); }
    },
    async cancel() {
      cancelled = true;
      await reader?.cancel().catch(() => {});
      await iterator.return(undefined);
    }
  }), { status, headers });
}
