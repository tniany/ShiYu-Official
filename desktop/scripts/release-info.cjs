const fs = require("node:fs/promises");
const path = require("node:path");
const crypto = require("node:crypto");

(async () => {
  const root = path.resolve(__dirname, "..");
  const release = path.resolve(root, "../release/windows");
  const version = require("../package.json").version;
  const name = `Shiyu-${version}-win-x64-Setup.exe`;
  const bytes = await fs.readFile(path.join(release, name));
  const sha256 = crypto.createHash("sha256").update(bytes).digest("hex");
  await fs.writeFile(path.join(release, "SHA256SUMS.txt"), `${sha256} *${name}\n`);
  await fs.copyFile(path.join(root, "resources/INSTALL.txt"), path.join(release, "\u4f7f\u7528\u8bf4\u660e.txt"));
  console.log(`Installer: ${name} (${(bytes.length / 1024 / 1024).toFixed(1)} MiB)\nSHA256: ${sha256}`);
})().catch(error => { console.error(error); process.exitCode = 1; });
