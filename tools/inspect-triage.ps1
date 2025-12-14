param(
  [string]$HtmlPath = ".\index.html",
  [string]$OutDir = ".\build\inspection"
)

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
if (!(Test-Path $HtmlPath)) { throw "File not found: $HtmlPath" }

# 1) Leggi HTML
$html = Get-Content $HtmlPath -Raw -Encoding UTF8

# 2) Totali warnings / weak warnings
$tot = [regex]::Match($html, "Inspection&nbsp;Results.*?(\d+)&nbsp;warnings\s+(\d+)&nbsp;weak&nbsp;warnings", "Singleline")
if ($tot.Success) {
  [PSCustomObject]@{
    warnings     = [int]$tot.Groups[1].Value
    weakWarnings = [int]$tot.Groups[2].Value
  } | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $OutDir "totals.json")
}

# 3) Estrai coppie (Inspection name -> count warnings) dall’albero
# NB: parsing “best effort” sull’HTML JetBrains
$rx = New-Object System.Text.RegularExpressions.Regex(
  "<b>(?<name>[^<]+)</b>&nbsp;inspection&nbsp;<span class=""grayout"">&nbsp;&nbsp;(?<count>\d+)&nbsp;warnings",
  [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)

$items = foreach ($m in $rx.Matches($html)) {
  [PSCustomObject]@{
    Inspection = ($m.Groups["name"].Value -replace "&nbsp;"," " -replace "&#39;","'")
    Count      = [int]$m.Groups["count"].Value
  }
}

$items |
  Group-Object Inspection |
  ForEach-Object {
    [PSCustomObject]@{ Inspection = $_.Name; Count = ($_.Group | Measure-Object Count -Sum).Sum }
  } |
  Sort-Object Count -Descending |
  Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutDir "top_inspections.csv")

Write-Host "Wrote: $OutDir\totals.json and top_inspections.csv" -ForegroundColor Green
Write-Host "Tip: apri top_inspections.csv e dimmi le prime 10 'code-related' (escludendo Typo/Proofreading)." -ForegroundColor Yellow
