#!/usr/bin/env pwsh

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-GradleValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Source,

        [Parameter(Mandatory = $true)]
        [string]$Pattern,

        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    $match = [regex]::Match($Source, $Pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if (-not $match.Success) {
        throw "Unable to find $Label in app/build.gradle.kts."
    }

    return $match.Groups[1].Value
}

function Convert-ToRepoUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Remote
    )

    if ($Remote -match '^git@([^:]+):(.+?)(?:\.git)?$') {
        return "https://$($Matches[1])/$($Matches[2]).git"
    }

    if ($Remote -match '^ssh://git@([^/]+)/(.+?)(?:\.git)?$') {
        return "https://$($Matches[1])/$($Matches[2]).git"
    }

    return $Remote
}

function Remove-GitSuffix {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    if ($Url.EndsWith('.git')) {
        return $Url.Substring(0, $Url.Length - 4)
    }

    return $Url
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

$buildFile = Get-Content -Raw -Path (Join-Path $repoRoot 'app/build.gradle.kts')
$applicationId = Get-GradleValue -Source $buildFile -Pattern 'applicationId\s*=\s*"([^"]+)"' -Label 'applicationId'
$versionCode = [int](Get-GradleValue -Source $buildFile -Pattern 'versionCode\s*=\s*(\d+)' -Label 'versionCode')
$versionName = Get-GradleValue -Source $buildFile -Pattern 'versionName\s*=\s*"([^"]+)"' -Label 'versionName'

$remote = (git remote get-url origin).Trim()
if (-not $remote) {
    throw 'Unable to read the origin remote URL.'
}

$repoUrl = Convert-ToRepoUrl -Remote $remote
$repoBaseUrl = Remove-GitSuffix -Url $repoUrl
$commitSha = (git rev-parse HEAD).Trim()
$expectedTag = "v$versionName"

Write-Host "Building release APK for $applicationId $versionName ($versionCode)"
& .\gradlew.bat :app:assembleRelease --no-daemon

$apkSource = Join-Path $repoRoot 'app/build/outputs/apk/release/app-release-unsigned.apk'
if (-not (Test-Path $apkSource)) {
    throw "Expected release APK not found at $apkSource"
}

$outputRoot = Join-Path $repoRoot 'build/fdroid'
$metadataDir = Join-Path $outputRoot 'metadata'
New-Item -ItemType Directory -Force -Path $metadataDir | Out-Null

$apkName = "Droidrops-$versionName-release-unsigned.apk"
$apkOutput = Join-Path $outputRoot $apkName
Copy-Item -LiteralPath $apkSource -Destination $apkOutput -Force

$apkHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apkOutput).Hash.ToLowerInvariant()

$metadataPath = Join-Path $metadataDir "$applicationId.yml"
$metadata = @"
AutoName: Droidrops
Categories:
  - Internet
License: GPL-3.0-only
WebSite: $repoBaseUrl
SourceCode: $repoBaseUrl
IssueTracker: $repoBaseUrl/issues
Summary: Droidrops RSS client
Description: |-
  Droidrops is a fork of Readrops.
  It is prepared as an independent F-Droid-friendly fork with a distinct application id, name, icon, and privacy policy.
RepoType: git
Repo: $repoUrl
UpdateCheckMode: Tags
AutoUpdateMode: Version
CurrentVersion: $versionName
CurrentVersionCode: $versionCode

Builds:
  - versionName: '$versionName'
    versionCode: $versionCode
    commit: $commitSha
    subdir: app
    gradle:
      - yes
"@

Set-Content -LiteralPath $metadataPath -Value $metadata -Encoding utf8

$reportPath = Join-Path $outputRoot 'release-info.txt'
$report = @"
ApplicationId: $applicationId
VersionName: $versionName
VersionCode: $versionCode
Commit: $commitSha
ExpectedTag: $expectedTag
Repo: $repoUrl
SourceCode: $repoBaseUrl
IssueTracker: $repoBaseUrl/issues
APK: $apkOutput
SHA256: $apkHash
"@

Set-Content -LiteralPath $reportPath -Value $report -Encoding utf8

Write-Host ''
Write-Host "F-Droid prep complete."
Write-Host "Metadata template: $metadataPath"
Write-Host "Release report:    $reportPath"
Write-Host "APK copy:          $apkOutput"
Write-Host ''
Write-Host 'Manual steps still required:'
Write-Host "1. Create/push the release tag $expectedTag if it is not already present."
Write-Host "2. Open https://gitlab.com/fdroid/fdroiddata and add metadata/$applicationId.yml there."
Write-Host "3. Run fdroid lint/build in fdroiddata, then open the merge request."
