param([switch]$Apply)

$ErrorActionPreference = "Stop"

function Assert-RepoRoot {
  if (-not (Test-Path "settings.gradle.kts") -and -not (Test-Path "settings.gradle")) {
    throw "Esegui dalla root del progetto."
  }
}

function Has-KtRef([string]$pattern) {
  $hits = Select-String -Path .\app\src\main\java\**\*.kt -Pattern $pattern -Quiet
  return [bool]$hits
}

Assert-RepoRoot

Write-Host "MODE: " -NoNewline
Write-Host ($(if($Apply){"APPLY"}else{"DRY-RUN"})) -ForegroundColor Magenta

# 1) Delete feature dir if no refs
$featureDir = "app\src\main\java\com\app\miklink\feature"
if (Test-Path $featureDir) {
  if (Has-KtRef "com\.app\.miklink\.feature") {
    Write-Host "SKIP: refs to com.app.miklink.feature still exist in main/java" -ForegroundColor Yellow
  } else {
    if ($Apply) { Remove-Item -Recurse -Force $featureDir }
    Write-Host "DELETE DIR: $featureDir" -ForegroundColor Green
  }
} else {
  Write-Host "OK: feature dir not found" -ForegroundColor DarkGreen
}

# 2) Delete duplicate TestProfileRepository root if no refs
$dupRepo = "app\src\main\java\com\app\miklink\core\data\repository\TestProfileRepository.kt"
if (Test-Path $dupRepo) {
  if (Has-KtRef "com\.app\.miklink\.core\.data\.repository\.TestProfileRepository") {
    Write-Host "SKIP: refs to duplicate TestProfileRepository still exist in main/java" -ForegroundColor Yellow
  } else {
    if ($Apply) { Remove-Item -Force $dupRepo }
    Write-Host "DELETE FILE: $dupRepo" -ForegroundColor Green
  }
} else {
  Write-Host "OK: duplicate TestProfileRepository file not found" -ForegroundColor DarkGreen
}

Write-Host "DONE." -ForegroundColor White
Write-Host "If OK, run: .\Cleanup-Now.ps1 -Apply" -ForegroundColor Magenta
