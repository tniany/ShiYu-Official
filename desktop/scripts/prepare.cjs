const fs = require("node:fs/promises");
const path = require("node:path");
const crypto = require("node:crypto");
const cheerio = require("cheerio");

async function prepare() {
  require("electron");
  const root = path.resolve(__dirname, "..");
  const web = path.resolve(root, "../web");
  const output = path.join(root, "renderer");
  await fs.mkdir(output, { recursive: true });
  await fs.mkdir(path.join(output, "assets"), { recursive: true });
  await fs.mkdir(path.join(output, "vendor"), { recursive: true });
  await fs.mkdir(path.join(root, "resources"), { recursive: true });

  const files = [
    "app.js", "styles.css", "manifest.webmanifest",
    "assets/character.png", "assets/icon-192.png", "assets/icon-512.png"
  ];
  const hashes = {};
  for (const file of ["index.html", ...files]) {
    const bytes = await fs.readFile(path.join(web, file));
    hashes[file] = crypto.createHash("sha256").update(bytes).digest("hex");
    if (file !== "index.html") await fs.writeFile(path.join(output, file), bytes);
  }
  const $ = cheerio.load(await fs.readFile(path.join(web, "index.html"), "utf8"));
  $("title").text("\u65f6\u9047");
  $('meta[name="application-name"]').attr("content", "\u65f6\u9047 Windows");
  $(".conversation-rail .search-field > span").text("Ctrl+K");
  $('link[href*="fonts.googleapis.com"], link[href*="fonts.gstatic.com"]').remove();
  $('script[src="https://unpkg.com/lucide@latest"]').attr("src", "./vendor/lucide.js");
  $("script:not([src])").each((_, script) => {
    if ($(script).text().includes("serviceWorker")) $(script).remove();
  });
  $("head").append('<link rel="stylesheet" href="./desktop.css">');
  await fs.writeFile(path.join(output, "index.html"), $.html());
  await fs.copyFile(path.join(root, "src/desktop.css"), path.join(output, "desktop.css"));
  await fs.copyFile(path.join(root, "node_modules/lucide/dist/umd/lucide.js"), path.join(output, "vendor/lucide.js"));
  await fs.copyFile(path.join(root, "node_modules/lucide/LICENSE"), path.join(output, "vendor/LUCIDE-LICENSE"));
  const icon = await fs.readFile(path.join(web, "assets/icon-512.png"));
  await fs.writeFile(path.join(root, "resources/icon.png"), icon);
  const { default: pngToIco } = await import("png-to-ico");
  await fs.writeFile(path.join(root, "resources/icon.ico"), await pngToIco(icon));
  await fs.writeFile(path.join(output, "source-manifest.json"), JSON.stringify({
    desktopVersion: require("../package.json").version,
    webVersion: $('meta[name="shiyu-version"]').attr("content"),
    source: "../web",
    sha256: hashes
  }, null, 2));
  console.log(`Bundled ${files.length + 1} web assets, local Lucide and Windows icons.`);
}

prepare().catch(error => { console.error(error); process.exitCode = 1; });
