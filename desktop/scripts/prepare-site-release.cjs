const fs = require("node:fs/promises");
const path = require("node:path");
const crypto = require("node:crypto");
const assert = require("node:assert/strict");

function replaceRequired(source, pattern, replacement, label) {
  assert.match(source, pattern, `${label} is missing from the download page`);
  return source.replace(pattern, replacement);
}

(async () => {
  const root = path.resolve(__dirname, "../..");
  const desktop = path.join(root, "desktop");
  const release = path.join(root, "release/windows");
  const official = path.join(root, "build/shiyu-official-windows-026");
  const download = path.join(official, "download");
  const version = JSON.parse(await fs.readFile(path.join(desktop, "package.json"), "utf8")).version;
  const filename = `Shiyu-${version}-win-x64-Setup.exe`;
  const installer = path.join(release, filename);
  const blockmap = `${installer}.blockmap`;
  const latest = path.join(release, "latest.yml");
  const bytes = await fs.readFile(installer);
  const sha256 = crypto.createHash("sha256").update(bytes).digest("hex");
  const sha512 = crypto.createHash("sha512").update(bytes).digest("base64");
  const latestYml = await fs.readFile(latest, "utf8");
  assert.match(latestYml, new RegExp(`version:\\s*${version.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}`));
  assert.ok(latestYml.includes(`path: ${filename}`), "latest.yml points to an unexpected installer");
  assert.ok(latestYml.includes(sha512), "latest.yml SHA-512 does not match the installer");
  await fs.access(blockmap);

  const partSize = 20 * 1024 * 1024;
  const parts = path.join(root, "web/downloads/windows");
  await fs.mkdir(parts, { recursive: true });
  for (const entry of await fs.readdir(parts)) {
    if (/^Shiyu-.*-win-x64-Setup\.exe\.part\d+$/.test(entry) && !entry.startsWith(`${filename}.part`)) {
      await fs.unlink(path.join(parts, entry));
    }
    if (/^Shiyu-.*-win-x64-Setup\.exe\.blockmap$/.test(entry) && entry !== `${filename}.blockmap`) {
      await fs.unlink(path.join(parts, entry));
    }
  }
  for (let offset = 0, index = 1; offset < bytes.length; offset += partSize, index++) {
    await fs.writeFile(path.join(parts, `${filename}.part${String(index).padStart(2, "0")}`), bytes.subarray(offset, offset + partSize));
  }
  await fs.copyFile(latest, path.join(parts, "latest.yml"));
  await fs.copyFile(blockmap, path.join(parts, `${filename}.blockmap`));
  await fs.copyFile(latest, path.join(download, "latest.yml"));
  await fs.copyFile(blockmap, path.join(download, `${filename}.blockmap`));

  await fs.copyFile(path.join(root, "desktop/test-results/desktop-home.png"), path.join(download, "windows-preview.png"));
  await fs.copyFile(path.join(official, "assets/app-icon-192.png"), path.join(download, "icon.png"));
  await fs.copyFile(path.join(root, "desktop/node_modules/lucide/dist/umd/lucide.js"), path.join(download, "lucide.js"));
  await fs.copyFile(path.join(root, "desktop/node_modules/lucide/LICENSE"), path.join(download, "LUCIDE-LICENSE"));
  await fs.writeFile(path.join(download, "SHA256SUMS.txt"), `${sha256} *${filename}\n`);
  await fs.copyFile(path.join(desktop, "resources/INSTALL.txt"), path.join(download, "INSTALL.txt"));

  const releaseTag = `windows-v${version}`;
  const localizedSize = bytes.length.toLocaleString("en-US");
  const pageFile = path.join(download, "index.html");
  let pageHtml = await fs.readFile(pageFile, "utf8");
  pageHtml = pageHtml.replace(/Shiyu-\d+\.\d+\.\d+-win-x64-Setup\.exe/g, filename);
  pageHtml = pageHtml.replace(/windows-v\d+\.\d+\.\d+/g, releaseTag);
  pageHtml = pageHtml.replace(/\b0\.2\.\d+\b/g, version);
  pageHtml = replaceRequired(pageHtml, /(id="checksum-value">)[0-9a-f]{64}/, `$1${sha256}`, "checksum");
  pageHtml = replaceRequired(pageHtml, /(<div><dt>大小<\/dt><dd>)[\d,]+ 字节/, `$1${localizedSize} 字节`, "installer size");
  await fs.writeFile(pageFile, pageHtml);
  for (const file of [path.join(official, "index.html"), path.join(official, "_redirects")]) {
    let html = await fs.readFile(file, "utf8");
    html = html.replace(/Shiyu-\d+\.\d+\.\d+-win-x64-Setup\.exe/g, filename);
    html = html.replace(/windows-v\d+\.\d+\.\d+/g, releaseTag);
    html = html.replace(/\b0\.2\.\d+\b/g, version);
    await fs.writeFile(file, html);
  }

  const manifest = {
    version, platform: "windows", arch: "x64", published_at: new Date().toISOString().slice(0, 10),
    filename, size: bytes.length, sha256,
    download_url: `https://web-shiy.tniay.top/downloads/windows/${filename}`,
    fallback_url: `https://github.com/tniany/shiyu-releases/releases/download/${releaseTag}/${filename}`,
    update_url: "https://web-shiy.tniay.top/downloads/windows/latest.yml",
    page_url: "https://shiy.tniay.top/download/",
    signed: false, minimum_os: "Windows 10"
  };
  await fs.writeFile(path.join(download, "windows.json"), JSON.stringify(manifest, null, 2) + "\n");

  // Public-site authoring stays in shiyu-official; only this self-contained
  // download page is copied to the separate Web deployment.
  await fs.cp(download, path.join(root, "web/download"), { recursive: true, force: true });
  console.log(`Prepared Windows ${version}: ${Math.ceil(bytes.length / partSize)} parts, latest.yml, blockmap and two independent download pages.`);
})().catch(error => { console.error(error); process.exitCode = 1; });
