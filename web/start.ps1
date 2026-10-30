param([int]$Port = 4173)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$noBrowser = $env:SHIYU_NO_BROWSER -eq "1"
$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add("http://localhost:$Port/")

try {
  $listener.Start()
} catch {
  Write-Host "端口 $Port 已被占用，请运行：.\start.ps1 -Port 4174"
  exit 1
}

if (!$noBrowser) { Start-Process "http://localhost:$Port/" }
Write-Host "时遇 Web 1.3.0 已启动：http://localhost:$Port/"
Write-Host "关闭此窗口即可停止服务。"

$mime = @{
  ".html" = "text/html; charset=utf-8"
  ".js" = "text/javascript; charset=utf-8"
  ".css" = "text/css; charset=utf-8"
  ".json" = "application/json; charset=utf-8"
  ".webmanifest" = "application/manifest+json; charset=utf-8"
  ".png" = "image/png"
  ".jpg" = "image/jpeg"
  ".jpeg" = "image/jpeg"
  ".svg" = "image/svg+xml"
  ".mp3" = "audio/mpeg"
  ".wav" = "audio/wav"
}

while ($listener.IsListening) {
  $context = $listener.GetContext()
  $path = [Uri]::UnescapeDataString($context.Request.Url.AbsolutePath.TrimStart("/"))
  if ([string]::IsNullOrWhiteSpace($path)) { $path = "index.html" }
  $candidate = [IO.Path]::GetFullPath((Join-Path $root $path))
  if (!$candidate.StartsWith([IO.Path]::GetFullPath($root), [StringComparison]::OrdinalIgnoreCase) -or !(Test-Path -LiteralPath $candidate -PathType Leaf)) {
    $candidate = Join-Path $root "index.html"
  }
  $bytes = [IO.File]::ReadAllBytes($candidate)
  $extension = [IO.Path]::GetExtension($candidate).ToLowerInvariant()
  $context.Response.ContentType = $mime[$extension] ?? "application/octet-stream"
  $context.Response.ContentLength64 = $bytes.Length
  $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
  $context.Response.OutputStream.Close()
}
