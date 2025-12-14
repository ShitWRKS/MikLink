param(
  [string]$Root = (Get-Location).Path,
  [string]$OutDir = (Join-Path (Get-Location).Path "_code_dump"),

  # Se > 0, spezza l’output in più dump (circa) di questa dimensione massima (in bytes)
  [long]$MaxBytesPerDump = 0,

  # Se attivo, crea anche dump separati per aree (domain / data / ui+di / docs)
  [switch]$SplitByArea,

  # Include binari come BASE64 se esplicitamente in allowlist (di default include wrapper jar)
  [switch]$IncludeAllowedBinaries,

  # Se attivo, include QUALSIASI binario come BASE64 fino a MaxBinaryBytes (sconsigliato)
  [switch]$IncludeAllBinaries,

  # Limite massimo per includere un binario in BASE64 (default 2MB)
  [long]$MaxBinaryBytes = 2MB
)

$ErrorActionPreference = "Stop"

function Ensure-Dir([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path)) {
    New-Item -ItemType Directory -Path $Path | Out-Null
  }
}

function Normalize-Rel([string]$Rel) {
  return $Rel.Trim().Replace("\","/")
}

# Esclusioni “strutturali”
$ExcludedPrefixes = @(
  "_code_dump/",
  ".git/",
  ".gradle/",
  ".idea/",
  ".kotlin/",
  ".vscode/",
  ".cursor/",
  ".continue/"
)

# Escludi build outputs ovunque
$ExcludedRegex = @(
  '(^|/)build/',
  '(^|/)out/',
  '(^|/)dist/',
  '(^|/)node_modules/',
  '(^|/)captures/',
  '(^|/)app/release/'
)

function Is-Excluded([string]$Rel) {
  $r = Normalize-Rel $Rel
  foreach ($p in $ExcludedPrefixes) {
    if ($r.StartsWith($p)) { return $true }
  }
  foreach ($rx in $ExcludedRegex) {
    if ($r -match $rx) { return $true }
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

function Get-Leaf([string]$Rel) {
  $r = Normalize-Rel $Rel
  return ($r -split "/")[-1]
}

function Get-Ext([string]$Leaf) {
  $m = [regex]::Match($Leaf, "\.[^./\\]+$")
  if (-not $m.Success) { return "" }
  return $m.Value.ToLowerInvariant()
}

# File senza estensione utili (testo)
$AllowedNoExtLeaves = @(
  "gradlew","gradlew.bat",
  ".gitignore",".gitattributes",".editorconfig",
  "gradle.properties","settings.gradle","settings.gradle.kts",
  "build.gradle","build.gradle.kts"
)

# Estensioni “testo”
$AllowedTextExt = @(
  ".kt",".kts",".java",
  ".gradle",".properties",".toml",
  ".xml",".json",".yml",".yaml",
  ".md",".txt",".pro",".rules",
  ".sh",".bat",".ps1",".cmd"
)

# Binari “grossi / junk” (se IncludeAllBinaries è OFF vengono sempre esclusi)
$BlockedBinaryExt = @(
  ".png",".jpg",".jpeg",".gif",".webp",".ico",
  ".zip",".aar",".apk",".aab",".so",".dll",".exe",
  ".pdf",".mp4",".mov",".avi",
  ".keystore",".jks",
  ".db",".sqlite",".realm",
  ".class"
)

# Allowlist binari “necessari” (includibili come BASE64)
$AllowedBinaryPaths = @(
  "gradle/wrapper/gradle-wrapper.jar"
)

function Try-ReadTextUtf8OrNull([string]$AbsPath) {
  try {
    $bytes = [IO.File]::ReadAllBytes($AbsPath)

    # euristica: NUL byte => binario
    foreach ($b in $bytes) { if ($b -eq 0) { return $null } }

    $utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
    return $utf8Strict.GetString($bytes)
  } catch {
    return $null
  }
}

function To-Base64Lines([byte[]]$Bytes, [int]$LineLen = 76) {
  $b64 = [Convert]::ToBase64String($Bytes)
  $lines = New-Object System.Collections.Generic.List[string]
  for ($i=0; $i -lt $b64.Length; $i += $LineLen) {
    $len = [Math]::Min($LineLen, $b64.Length - $i)
    $lines.Add($b64.Substring($i, $len))
  }
  return $lines
}

function Get-Sha256([byte[]]$Bytes) {
  $sha = [System.Security.Cryptography.SHA256]::Create()
  $hash = $sha.ComputeHash($Bytes)
  return ($hash | ForEach-Object { $_.ToString("x2") }) -join ""
}

function Write-DumpsWithSplit(
  [string]$BaseName,
  [string[]]$RelFiles,
  [ref]$ManifestOut,
  [string]$RootDir,
  [string]$OutDirectory,
  [long]$MaxBytes,
  [switch]$InclAllowedBin,
  [switch]$InclAllBin,
  [long]$MaxBinBytes
) {
  if (-not $RelFiles -or $RelFiles.Count -eq 0) { return }

  $dumpIndex = 1
  $currentBytes = 0
  $writer = $null
  $outPath = $null

  # Scriptblock dot-sourced -> mantiene scope (writer/outPath/currentBytes)
  $OpenNewWriter = {
    if ($null -ne $writer) {
      try { $writer.Flush() } catch {}
      try { $writer.Close() } catch {}
      $writer = $null
    }

    $suffix = if ($MaxBytes -gt 0) { "_{0:D4}" -f $dumpIndex } else { "" }
    $outPath = Join-Path $OutDirectory ("{0}{1}.txt" -f $BaseName, $suffix)

    if (Test-Path -LiteralPath $outPath) { Remove-Item -LiteralPath $outPath -Force }

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    $writer = New-Object System.IO.StreamWriter($outPath, $true, $utf8NoBom)

    $currentBytes = 0
    Write-Host "Wrote: $outPath"
  }

  . $OpenNewWriter

  foreach ($rel in $RelFiles) {
    $r = Normalize-Rel $rel
    $abs = Join-Path $RootDir $r
    if (-not (Test-Path -LiteralPath $abs)) { continue }

    # bytes per sha/size
    $bytes = $null
    try { $bytes = [IO.File]::ReadAllBytes($abs) } catch { continue }
    $size = $bytes.Length
    $sha256 = Get-Sha256 $bytes

    $leaf = Get-Leaf $r
    $ext  = Get-Ext $leaf

    $type = "TEXT"
    $contentText = $null

    $isAllowedBinary = ($AllowedBinaryPaths -contains $r)

    # Decide formato:
    # 1) allowlist binari + IncludeAllowedBinaries => BASE64
    # 2) altrimenti prova TEXT (UTF8 strict)
    # 3) se fallisce e IncludeAllBinaries e size <= MaxBinaryBytes => BASE64
    # 4) altrimenti skip
    if ($isAllowedBinary -and $InclAllowedBin) {
      $type = "BASE64"
    } else {
      $contentText = Try-ReadTextUtf8OrNull $abs
      if ($null -eq $contentText) {
        if ($InclAllBin -and ($size -le $MaxBinBytes)) {
          $type = "BASE64"
        } else {
          $r | Out-File -FilePath (Join-Path $OutDirectory "skipped_paths.txt") -Append -Encoding UTF8
          continue
        }
      }
    }

    # Split per dimensione
    $payloadEstimate = 0
    if ($type -eq "TEXT") {
      $payloadEstimate = [Text.Encoding]::UTF8.GetByteCount($contentText)
    } else {
      $payloadEstimate = [Math]::Ceiling($size * 1.37) # base64 approx
    }
    $estimated = 400 + $payloadEstimate

    if ($MaxBytes -gt 0 -and ($currentBytes + $estimated) -gt $MaxBytes -and $currentBytes -gt 0) {
      $dumpIndex++
      . $OpenNewWriter
    }

    if ($null -eq $writer) {
      throw "Internal error: writer is null (should never happen)."
    }

    # Header file
    $writer.WriteLine("")
    $writer.WriteLine("==================== BEGIN FILE ====================")
    $writer.WriteLine("PATH: $r")
    $writer.WriteLine("TYPE: $type")
    $writer.WriteLine("SIZE: $size")
    $writer.WriteLine("SHA256: $sha256")
    $writer.WriteLine("==================== END HEADER ====================")

    if ($type -eq "TEXT") {
      $writer.WriteLine($contentText)
    } else {
      $writer.WriteLine("BEGIN_BASE64")
      $b64Lines = To-Base64Lines $bytes 76
      foreach ($line in $b64Lines) { $writer.WriteLine($line) }
      $writer.WriteLine("END_BASE64")
    }

    $writer.WriteLine("==================== END FILE ====================")

    $currentBytes += $estimated

    $ManifestOut.Value += [pscustomobject]@{
      path     = $r
      type     = $type
      size     = $size
      sha256   = $sha256
      dumpFile = (Split-Path -Leaf $outPath)
    }
  }

  if ($null -ne $writer) {
    try { $writer.Flush() } catch {}
    try { $writer.Close() } catch {}
  }
}

# -------------------- MAIN --------------------

$Root = (Resolve-Path -LiteralPath $Root).Path

# Se OutDir è relativo, lo mettiamo sotto Root
if (-not [IO.Path]::IsPathRooted($OutDir)) {
  $OutDir = Join-Path $Root $OutDir
}
Ensure-Dir $OutDir

$skippedLog = Join-Path $OutDir "skipped_paths.txt"
if (Test-Path -LiteralPath $skippedLog) { Remove-Item -LiteralPath $skippedLog -Force }

Push-Location $Root
try {
  git rev-parse --is-inside-work-tree *> $null
} catch {
  Pop-Location
  throw "Git repo not detected. Esegui lo script dalla root del repo."
}

# tracked + untracked non ignorati
$files = git ls-files -co --exclude-standard
Pop-Location

# Filtra
$clean = New-Object System.Collections.Generic.List[string]
foreach ($f in $files) {
  $rel = Normalize-Rel $f
  if ($rel -eq "") { continue }
  if (Is-Excluded $rel) { continue }

  if (Has-InvalidWindowsChars $rel) {
    $rel | Out-File -FilePath $skippedLog -Append -Encoding UTF8
    continue
  }

  $leaf = Get-Leaf $rel
  $ext  = Get-Ext $leaf

  $isNoExtAllowed = ($leaf -in $AllowedNoExtLeaves)
  $isTextExtAllowed = ($ext -ne "" -and ($AllowedTextExt -contains $ext))
  $isAllowedBinary = ($AllowedBinaryPaths -contains $rel)
  $isBlockedBinary = ($ext -ne "" -and ($BlockedBinaryExt -contains $ext))

  if ($isTextExtAllowed -or $isNoExtAllowed) {
    $clean.Add($rel)
  } elseif ($isAllowedBinary -and $IncludeAllowedBinaries) {
    $clean.Add($rel)
  } elseif ($IncludeAllBinaries) {
    # includi tutto, ma evita comunque i “blocked” enormi/junk se vuoi (qui li includiamo, poi verranno skippati se > MaxBinaryBytes)
    $clean.Add($rel)
  } else {
    # niente
    continue
  }

  # Se non includiamo binari e l’estensione è bloccata, taglia subito
  if (-not $IncludeAllBinaries -and -not ($isAllowedBinary -and $IncludeAllowedBinaries) -and $isBlockedBinary) {
    # rimuovi quello appena aggiunto
    $null = $clean.Remove($rel)
  }
}

$files = $clean.ToArray() | Sort-Object

$manifest = @()

Write-DumpsWithSplit `
  -BaseName "dump_all" `
  -RelFiles $files `
  -ManifestOut ([ref]$manifest) `
  -RootDir $Root `
  -OutDirectory $OutDir `
  -MaxBytes $MaxBytesPerDump `
  -InclAllowedBin:$IncludeAllowedBinaries `
  -InclAllBin:$IncludeAllBinaries `
  -MaxBinBytes $MaxBinaryBytes

if ($SplitByArea) {
  $norm = { param($p) (Normalize-Rel $p) }

  $coreDomain = $files | Where-Object { (&$norm $_) -match "app/src/main/java/.+/core/domain/" }
  $dataImpl   = $files | Where-Object { (&$norm $_) -match "app/src/main/java/.+/(data/|core/data/)" }
  $uiDi       = $files | Where-Object { (&$norm $_) -match "app/src/main/java/.+/(ui/|di/)" }
  $docs       = $files | Where-Object { (&$norm $_) -match "^docs/" }

  Write-DumpsWithSplit -BaseName "dump_core_domain" -RelFiles $coreDomain -ManifestOut ([ref]$manifest) -RootDir $Root -OutDirectory $OutDir -MaxBytes $MaxBytesPerDump -InclAllowedBin:$IncludeAllowedBinaries -InclAllBin:$IncludeAllBinaries -MaxBinBytes $MaxBinaryBytes
  Write-DumpsWithSplit -BaseName "dump_data_impl"   -RelFiles $dataImpl   -ManifestOut ([ref]$manifest) -RootDir $Root -OutDirectory $OutDir -MaxBytes $MaxBytesPerDump -InclAllowedBin:$IncludeAllowedBinaries -InclAllBin:$IncludeAllBinaries -MaxBinBytes $MaxBinaryBytes
  Write-DumpsWithSplit -BaseName "dump_ui_di"       -RelFiles $uiDi       -ManifestOut ([ref]$manifest) -RootDir $Root -OutDirectory $OutDir -MaxBytes $MaxBytesPerDump -InclAllowedBin:$IncludeAllowedBinaries -InclAllBin:$IncludeAllBinaries -MaxBinBytes $MaxBinaryBytes
  Write-DumpsWithSplit -BaseName "dump_docs"        -RelFiles $docs       -ManifestOut ([ref]$manifest) -RootDir $Root -OutDirectory $OutDir -MaxBytes $MaxBytesPerDump -InclAllowedBin:$IncludeAllowedBinaries -InclAllBin:$IncludeAllBinaries -MaxBinBytes $MaxBinaryBytes
}

$manifestPath = Join-Path $OutDir "manifest.json"
$manifest | ConvertTo-Json -Depth 6 | Out-File -FilePath $manifestPath -Encoding UTF8

Write-Host "Done. Output dir: $OutDir"
Write-Host "Manifest: $manifestPath"
if (Test-Path -LiteralPath $skippedLog) { Write-Host "Skipped some paths. See: $skippedLog" }
