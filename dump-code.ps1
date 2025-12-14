param(
  [string]$Root = (Get-Location).Path,
  [string]$OutDir = (Join-Path (Get-Location).Path "_code_dump"),
  [switch]$SplitByArea
)

$ErrorActionPreference = "Stop"

function Ensure-Dir([string]$Path) {
  if (-not (Test-Path $Path)) { New-Item -ItemType Directory -Path $Path | Out-Null }
}

# Cartelle da escludere a prescindere (aggiungi/togli se vuoi)
$ExcludedPrefixes = @(
  "_code_dump/",
  "Branding/",
  "app/release/",
  ".kotlin/",
  ".cursor/",
  ".continue/",
  ".vscode/"
)

function Is-Excluded([string]$Rel) {
  $r = $Rel.Replace("\","/")
  foreach ($p in $ExcludedPrefixes) {
    if ($r.StartsWith($p)) { return $true }
  }
  return $false
}

function Has-InvalidWindowsChars([string]$Rel) {
  if ([string]::IsNullOrWhiteSpace($Rel)) { return $true }
  $invalid = [IO.Path]::GetInvalidPathChars()
  foreach ($ch in $invalid) {
    if ($Rel.IndexOf($ch) -ge 0) { return $true }
  }
  return $false
}

function Is-CodeFile([string]$Rel) {
  $leaf = ($Rel.Replace("\","/") -split "/")[-1]

  # file "senza estensione" utili
  if ($leaf -in @(".gitignore",".gitattributes",".editorconfig","gradle.properties")) { return $true }

  # Estensione via regex (non Path.GetExtension)
  $m = [regex]::Match($leaf, "\.[^./\\]+$")
  if (-not $m.Success) { return $false }
  $ext = $m.Value.ToLowerInvariant()

  $allowed = @(
    ".kt",".kts",".java",
    ".gradle",".properties",".toml",
    ".xml",".json",".yml",".yaml",
    ".md",".txt",".pro"
  )
  return $allowed -contains $ext
}

function Is-BinaryOrJunk([string]$Rel) {
  $leaf = ($Rel.Replace("\","/") -split "/")[-1]
  $m = [regex]::Match($leaf, "\.[^./\\]+$")
  if (-not $m.Success) { return $false } # se non ha estensione, non la blocchiamo qui
  $ext = $m.Value.ToLowerInvariant()

  $blocked = @(
    ".png",".jpg",".jpeg",".gif",".webp",".ico",
    ".zip",".jar",".aar",".apk",".aab",".so",".dll",".exe",
    ".pdf",".mp4",".mov",".avi",
    ".keystore",".jks",
    ".db",".sqlite",".realm",
    ".class"
  )
  return $blocked -contains $ext
}

function Write-Dump([string]$OutFile, [string[]]$FileList) {
  $outPath = Join-Path $OutDir $OutFile
  Remove-Item $outPath -ErrorAction SilentlyContinue

  foreach ($rel in $FileList) {
    $abs = Join-Path $Root $rel
    if (-not (Test-Path $abs)) { continue }

    "`n`n==================== FILE: $rel ====================`n" | Out-File -FilePath $outPath -Append -Encoding UTF8

    try {
      Get-Content -Path $abs -Raw -Encoding UTF8 | Out-File -FilePath $outPath -Append -Encoding UTF8
    } catch {
      "`n[READ ERROR] $($_.Exception.Message)`n" | Out-File -FilePath $outPath -Append -Encoding UTF8
    }
  }

  Write-Host "Wrote: $outPath"
}

Ensure-Dir $OutDir
$skippedLog = Join-Path $OutDir "skipped_paths.txt"
Remove-Item $skippedLog -ErrorAction SilentlyContinue

Push-Location $Root
try {
  git rev-parse --is-inside-work-tree *> $null
} catch {
  Pop-Location
  throw "Git repo not detected. Esegui lo script dalla root del repo."
}

# elenco file rispettando .gitignore: tracked + untracked non ignorati
$files = git ls-files -co --exclude-standard

Pop-Location

$clean = New-Object System.Collections.Generic.List[string]
foreach ($f in $files) {
  $rel = $f.Trim()
  if ($rel -eq "") { continue }
  if (Is-Excluded $rel) { continue }

  if (Has-InvalidWindowsChars $rel) {
    $rel | Out-File -FilePath $skippedLog -Append -Encoding UTF8
    continue
  }

  if (Is-BinaryOrJunk $rel) { continue }
  if (-not (Is-CodeFile $rel)) { continue }

  $clean.Add($rel)
}
$files = $clean.ToArray()

Write-Dump -OutFile "dump_all.txt" -FileList $files

if ($SplitByArea) {
  $coreDomain = $files | Where-Object { $_.Replace("\","/") -match "app/src/main/java/.+/core/domain/" }
  $dataImpl   = $files | Where-Object { $_.Replace("\","/") -match "app/src/main/java/.+/(data/|core/data/)" }
  $uiDi       = $files | Where-Object { $_.Replace("\","/") -match "app/src/main/java/.+/(ui/|di/)" }

  Write-Dump -OutFile "dump_core_domain.txt" -FileList $coreDomain
  Write-Dump -OutFile "dump_data_impl.txt"   -FileList $dataImpl
  Write-Dump -OutFile "dump_ui_di.txt"       -FileList $uiDi
}

Write-Host "Done. Output dir: $OutDir"
if (Test-Path $skippedLog) { Write-Host "Skipped some paths (invalid on Windows). See: $skippedLog" }
