#!/usr/bin/env pwsh

param(
    [string]$RepoPath = (Join-Path (Join-Path $PSScriptRoot '..\..') 'fdroiddata')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoPathResolved = Resolve-Path -LiteralPath $RepoPath -ErrorAction SilentlyContinue
$repoPath = if ($repoPathResolved) { $repoPathResolved.Path } else { $RepoPath }

Write-Host "fdroiddata path: $RepoPath"

if (-not (Test-Path $RepoPath)) {
    Write-Host "Status: missing"
    exit 0
}

$gitDir = Join-Path $RepoPath '.git'
$indexLock = Join-Path $gitDir 'index.lock'

$activeGit = Get-CimInstance Win32_Process -Filter "Name = 'git.exe'" |
    Where-Object { $_.CommandLine -and $_.CommandLine -like "*fdroiddata*" } |
    Select-Object ProcessId, CreationDate, CommandLine

if ($activeGit) {
    Write-Host "Active git processes:"
    $activeGit | ForEach-Object {
        Write-Host "  PID $($_.ProcessId) started $($_.CreationDate)"
        Write-Host "    $($_.CommandLine)"
    }
} else {
    Write-Host "Active git processes: none"
}

Write-Host "Lock file: $(if (Test-Path $indexLock) { 'present' } else { 'none' })"

try {
    $inside = git -C $RepoPath rev-parse --is-inside-work-tree 2>$null
} catch {
    $inside = $null
}

if ($inside -ne 'true') {
    Write-Host "Git repo: no"
    exit 0
}

$branch = git -C $RepoPath branch --show-current 2>$null
$head = git -C $RepoPath rev-parse --short HEAD 2>$null
$trackedCount = @(git -C $RepoPath ls-tree -r --name-only HEAD 2>$null).Count
$presentFiles = @(Get-ChildItem -Path $RepoPath -Recurse -File -Force -ErrorAction SilentlyContinue).Count
$percent = if ($trackedCount -gt 0) {
    [Math]::Round([Math]::Min(100, ($presentFiles / $trackedCount) * 100), 1)
} else {
    0
}

Write-Host "Git repo: yes"
Write-Host "Branch: $branch"
Write-Host "HEAD: $head"
Write-Host "Tracked files: $trackedCount"
Write-Host "Present files: $presentFiles"
Write-Host "Approx. checkout completeness: $percent`%"

$statusLines = @(git -C $RepoPath status --porcelain=v1 --untracked-files=no 2>$null)
Write-Host "Status entries: $($statusLines.Count)"
if ($statusLines.Count -gt 0) {
    Write-Host "First entries:"
    $statusLines | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
}
