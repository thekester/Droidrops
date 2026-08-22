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

function Get-LocalPropertiesMap {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $values = @{}
    if (-not (Test-Path $Path)) {
        return $values
    }

    foreach ($line in Get-Content -Path $Path) {
        if ($line -match '^\s*([^#!][^=]+?)\s*=\s*(.*)\s*$') {
            $values[$matches[1].Trim()] = $matches[2].Trim()
        }
    }

    return $values
}

function Get-ReleaseSigningFingerprint {
    param(
        [Parameter(Mandatory = $true)]
        [string]$StoreFile,

        [Parameter(Mandatory = $true)]
        [string]$StorePassword,

        [Parameter(Mandatory = $true)]
        [string]$KeyAlias
    )

    $stdout = [System.IO.Path]::GetTempFileName()
    $stderr = [System.IO.Path]::GetTempFileName()
    try {
        $process = Start-Process -FilePath keytool -ArgumentList @(
            '-list'
            '-v'
            '-keystore'
            $StoreFile
            '-storepass'
            $StorePassword
            '-alias'
            $KeyAlias
        ) -NoNewWindow -Wait -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr

        if ($process.ExitCode -ne 0) {
            $errorText = Get-Content -Raw -Path $stderr
            throw "Unable to read the release signing certificate from $StoreFile. $errorText"
        }

        $text = Get-Content -Raw -Path $stdout
    } finally {
        Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue
    }

    if ([string]::IsNullOrWhiteSpace($text)) {
        throw "Unable to read the release signing certificate from $StoreFile."
    }

    $match = [regex]::Match($text, 'SHA\s*256:\s*([0-9A-F:]+)', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $match.Success) {
        throw "Unable to find the SHA-256 fingerprint in the release signing certificate at $StoreFile."
    }

    return ($match.Groups[1].Value -replace ':', '').ToLowerInvariant()
}

function Get-ApkSignerPath {
    $sdkRoots = @($env:ANDROID_SDK_ROOT, $env:ANDROID_HOME) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    foreach ($sdkRoot in $sdkRoots) {
        $buildToolsRoot = Join-Path $sdkRoot 'build-tools'
        if (-not (Test-Path $buildToolsRoot)) {
            continue
        }

        foreach ($buildToolsDir in (Get-ChildItem -Path $buildToolsRoot -Directory | Sort-Object Name -Descending)) {
            $candidate = Join-Path $buildToolsDir.FullName 'apksigner.bat'
            if (Test-Path $candidate) {
                return $candidate
            }
        }
    }

    throw 'Unable to find apksigner.bat. Set ANDROID_SDK_ROOT or ANDROID_HOME.'
}

function Write-Utf8LfFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [Parameter(Mandatory = $true)]
        [string]$Content
    )

    $normalized = ($Content -replace "`r", "").TrimEnd("`n") + "`n"
    [System.IO.File]::WriteAllText($Path, $normalized, [System.Text.UTF8Encoding]::new($false))
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
$localProperties = Get-LocalPropertiesMap -Path (Join-Path $repoRoot 'local.properties')
$releaseStoreFile = $localProperties['release.storeFile']
$releaseStorePassword = $localProperties['release.storePassword']
$releaseKeyAlias = $localProperties['release.keyAlias']
$releaseKeyPassword = $localProperties['release.keyPassword']
$hasReleaseSigningConfig = -not [string]::IsNullOrWhiteSpace($releaseStoreFile) -and
    -not [string]::IsNullOrWhiteSpace($releaseStorePassword) -and
    -not [string]::IsNullOrWhiteSpace($releaseKeyAlias) -and
    -not [string]::IsNullOrWhiteSpace($releaseKeyPassword)

Write-Host "Building release APK for $applicationId $versionName ($versionCode)"
& .\gradlew.bat :app:assembleRelease --no-daemon -PfdroidUnsigned=true

$apkUnsignedSource = Join-Path $repoRoot 'app/build/outputs/apk/release/app-release-unsigned.apk'
$apkSignedSource = Join-Path $repoRoot 'app/build/outputs/apk/release/app-release.apk'
$apksignerPath = if ($hasReleaseSigningConfig) {
    Get-ApkSignerPath
} else {
    $null
}

$outputRoot = Join-Path $repoRoot 'build/fdroid'
$metadataDir = Join-Path $outputRoot 'metadata'
New-Item -ItemType Directory -Force -Path $metadataDir | Out-Null

$isSignedRelease = $false
$apkName = if ($hasReleaseSigningConfig) {
    "Droidrops-$versionName-release.apk"
} else {
    "Droidrops-$versionName-release-unsigned.apk"
}
$apkOutput = Join-Path $outputRoot $apkName

if ($hasReleaseSigningConfig) {
    if (-not (Test-Path $apkUnsignedSource)) {
        throw "Expected unsigned release APK not found at $apkUnsignedSource"
    }

    $releaseStorePath = if ([System.IO.Path]::IsPathRooted($releaseStoreFile)) {
        $releaseStoreFile
    } else {
        Join-Path $repoRoot $releaseStoreFile
    }

    $signArgs = @(
        'sign'
        '--ks'
        $releaseStorePath
        '--ks-pass'
        "pass:$releaseStorePassword"
        '--key-pass'
        "pass:$releaseKeyPassword"
        '--alignment-preserved'
        '--out'
        $apkOutput
        $apkUnsignedSource
    )

    & $apksignerPath @signArgs
    if ($LASTEXITCODE -ne 0) {
        throw "apksigner failed while creating $apkOutput"
    }

    $isSignedRelease = $true
} elseif (Test-Path $apkUnsignedSource) {
    Copy-Item -LiteralPath $apkUnsignedSource -Destination $apkOutput -Force
} elseif (Test-Path $apkSignedSource) {
    Copy-Item -LiteralPath $apkSignedSource -Destination $apkOutput -Force
    $isSignedRelease = $true
} else {
    throw "Expected release APK not found at $apkSignedSource or $apkUnsignedSource"
}

$apkHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $apkOutput).Hash.ToLowerInvariant()
$binariesUrl = if ($isSignedRelease) {
    'https://github.com/thekester/Droidrops/releases/download/v%v/Droidrops-%v-release.apk'
} else {
    $null
}
$allowedApkSigningKey = if ($isSignedRelease -and $hasReleaseSigningConfig) {
    $releaseStorePath = if ([System.IO.Path]::IsPathRooted($releaseStoreFile)) {
        $releaseStoreFile
    } else {
        Join-Path $repoRoot $releaseStoreFile
    }

    Get-ReleaseSigningFingerprint -StoreFile $releaseStorePath -StorePassword $releaseStorePassword -KeyAlias $releaseKeyAlias
} else {
    $null
}

$metadataPath = Join-Path $metadataDir "$applicationId.yml"
$metadataLines = @(
    'Categories:'
    '  - News'
    'License: GPL-3.0-only'
    'AuthorName: Theophile Avenel'
    'AuthorEmail: theophile.avenel@gmail.com'
    'AuthorWebSite: https://tavenel.fr/html/homepage.html'
    "WebSite: $repoBaseUrl"
    "SourceCode: $repoBaseUrl"
    "IssueTracker: $repoBaseUrl/issues"
    ''
    'AutoName: Droidrops'
    ''
    'RepoType: git'
    "Repo: $repoUrl"
)

if ($binariesUrl) {
    $metadataLines += 'Binaries: '
    $metadataLines += "  $binariesUrl"
}

$metadataLines += ''
$metadataLines += 'Builds:'
$metadataLines += "  - versionName: $versionName"
$metadataLines += "    versionCode: $versionCode"
$metadataLines += "    commit: $commitSha"
$metadataLines += '    subdir: app'
$metadataLines += '    gradle:'
$metadataLines += '      - yes'

if ($allowedApkSigningKey) {
    $metadataLines += ''
    $metadataLines += "AllowedAPKSigningKeys: $allowedApkSigningKey"
}

$metadataLines += ''
$metadataLines += 'AutoUpdateMode: Version'
$metadataLines += 'UpdateCheckMode: Tags'
$metadataLines += "CurrentVersion: $versionName"
$metadataLines += "CurrentVersionCode: $versionCode"

$metadata = $metadataLines -join "`n"

Write-Utf8LfFile -Path $metadataPath -Content $metadata

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
SignedRelease: $isSignedRelease
Binaries: $binariesUrl
AllowedAPKSigningKeys: $allowedApkSigningKey
"@

Set-Content -LiteralPath $reportPath -Value $report -Encoding utf8

Write-Host ''
Write-Host "F-Droid prep complete."
Write-Host "Metadata template: $metadataPath"
Write-Host "Release report:    $reportPath"
Write-Host "APK copy:          $apkOutput"
if ($binariesUrl) {
    Write-Host "Binaries URL:      $binariesUrl"
}
if ($allowedApkSigningKey) {
    Write-Host "Signing key SHA:   $allowedApkSigningKey"
}
Write-Host ''
Write-Host 'Manual steps still required:'
Write-Host "1. Create/push the release tag $expectedTag if it is not already present."
Write-Host "2. If you use reproducible builds, upload the signed APK $apkName to the matching GitHub release tag."
Write-Host "3. Open https://gitlab.com/fdroid/fdroiddata and add metadata/$applicationId.yml there."
Write-Host "4. Keep summary/description in fastlane/metadata/android/en-US/ in this repo."
Write-Host "5. Run fdroid lint/build in fdroiddata, then open the merge request."
