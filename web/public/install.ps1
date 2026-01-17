$owner = "amak-tech"
$repo = "port-buddy"

Write-Host "Detecting latest version..." -ForegroundColor Cyan
$releaseUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
$release = Invoke-RestMethod -Uri $releaseUrl
$version = $release.tag_name

if (-not $version) {
    Write-Error "Could not determine the latest version."
    exit 1
}

$url = "https://github.com/$owner/$repo/releases/download/$version/portbuddy-windows-x64.exe"
$installDir = Join-Path $HOME ".portbuddy"
$exePath = Join-Path $installDir "portbuddy.exe"

if (-not (Test-Path $installDir)) {
    New-Item -ItemType Directory -Path $installDir | Out-Null
}

Write-Host "Downloading Port Buddy $version..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $url -OutFile $exePath

$path = [Environment]::GetEnvironmentVariable("Path", "User")
if ($path -notlike "*$installDir*") {
    Write-Host "Adding $installDir to PATH..." -ForegroundColor Cyan
    [Environment]::SetEnvironmentVariable("Path", "$path;$installDir", "User")
    $env:Path += ";$installDir"
}

Write-Host "Port Buddy installed successfully!" -ForegroundColor Green
Write-Host "Please restart your terminal to start using 'portbuddy'." -ForegroundColor Yellow
