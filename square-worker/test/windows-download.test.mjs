import test from "node:test";
import assert from "node:assert/strict";
import { WINDOWS_RELEASE, parseRange, streamWindows } from "../src/windows-download.ts";

const data = Uint8Array.from({ length: 29 }, (_, index) => index);
const release = { ...WINDOWS_RELEASE, size: data.length, partSize: 10 };
function mockAssets(useRange = true, truncate = false) {
  const calls = [];
  return {
    calls,
    async fetch(request) {
      calls.push(request);
      const part = Number(new URL(request.url).pathname.match(/part(\d+)$/)[1]) - 1;
      let bytes = data.slice(part * release.partSize, (part + 1) * release.partSize);
      if (useRange) {
        const range = parseRange(request.headers.get("range"), bytes.length);
        bytes = bytes.slice(range.start, range.end + 1);
      }
      if (truncate) bytes = bytes.slice(0, 1);
      return new Response(bytes, { status: useRange ? 206 : 200 });
    }
  };
}
const request = (headers = {}, method = "GET") => new Request(`https://web-shiy.tniay.top/downloads/windows/${release.filename}`, { headers, method });

test("single, open-ended, suffix and invalid ranges", () => {
  assert.deepEqual(parseRange("bytes=7-12", 29), { start: 7, end: 12 });
  assert.deepEqual(parseRange("bytes=27-", 29), { start: 27, end: 28 });
  assert.deepEqual(parseRange("bytes=-4", 29), { start: 25, end: 28 });
  assert.deepEqual(parseRange("bytes=25-999", 29), { start: 25, end: 28 });
  for (const invalid of ["bytes=30-", "bytes=4-2", "bytes=-0", "bytes=-", "bytes=1-2,4-5", "bad", "bytes=9007199254740992-"]) {
    assert.equal(parseRange(invalid, 29), null, invalid);
  }
});

for (const useRange of [true, false]) {
  test(`full binary and cross-part range preserve exact bytes (asset ranges=${useRange})`, async () => {
    const assets = mockAssets(useRange);
    const full = await streamWindows(request(), assets, release);
    assert.equal(full.status, 200);
    assert.equal(full.headers.get("content-length"), "29");
    assert.match(full.headers.get("content-disposition"), /attachment; filename="Shiyu/);
    assert.deepEqual(new Uint8Array(await full.arrayBuffer()), data);
    assert.equal(assets.calls.length, 3);
    for (const [range, start, end] of [["bytes=8-22", 8, 23], ["bytes=-2", 27, 29], ["bytes=10-10", 10, 11]]) {
      const response = await streamWindows(request({ range }), assets, release);
      assert.equal(response.status, 206);
      assert.equal(response.headers.get("content-range"), `bytes ${start}-${end - 1}/29`);
      assert.deepEqual(new Uint8Array(await response.arrayBuffer()), data.slice(start, end));
    }
  });
}

test("HEAD, conditional requests and invalid ranges do not read files", async () => {
  const assets = mockAssets();
  assert.equal((await streamWindows(request({}, "HEAD"), assets, release)).status, 200);
  assert.equal((await streamWindows(request({ "if-none-match": `"${release.sha256}"` }), assets, release)).status, 304);
  const invalid = await streamWindows(request({ range: "bytes=29-" }), assets, release);
  assert.equal(invalid.status, 416);
  assert.equal(invalid.headers.get("content-range"), "bytes */29");
  assert.equal(assets.calls.length, 0);
  const stale = await streamWindows(request({ range: "bytes=0-1", "if-range": '"old"' }), assets, release);
  assert.equal(stale.status, 200);
  assert.deepEqual(new Uint8Array(await stale.arrayBuffer()), data);
});

test("truncated or missing asset fails the download instead of emitting a corrupt successful body", async () => {
  for (const assets of [mockAssets(true, true), { fetch: async () => new Response("missing", { status: 404 }) }]) {
    const response = await streamWindows(request(), assets, release);
    await assert.rejects(response.arrayBuffer(), /Incomplete|unavailable/);
  }
});

test("cancelling stops the asset reader without fetching later parts", async () => {
  const assets = mockAssets();
  const response = await streamWindows(request(), assets, release);
  const reader = response.body.getReader();
  await reader.read();
  await reader.cancel();
  assert.equal(assets.calls.length, 1);
});
