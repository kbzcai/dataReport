param(
    [switch]$IncludeConfig
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$relativeFiles = @(
    'AGENTS.md',
    '.codex\agents\product.toml',
    '.codex\agents\frontend.toml',
    '.codex\agents\backend.toml',
    '.codex\agents\tester.toml',
    '.codex\agents\repoops.toml'
)

if ($IncludeConfig) {
    $relativeFiles += '.codex\config.toml'
}

foreach ($relativeFile in $relativeFiles) {
    $source = Join-Path $projectRoot $relativeFile
    $target = Join-Path $PSScriptRoot $relativeFile
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Source file does not exist: $source"
    }
    $targetDirectory = Split-Path -Parent $target
    New-Item -ItemType Directory -Force -Path $targetDirectory | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
}

if ($IncludeConfig) {
    Write-Output 'Multi-agent template files and config synchronized.'
} else {
    Write-Output 'Multi-agent template files synchronized. Local config was not copied.'
}
