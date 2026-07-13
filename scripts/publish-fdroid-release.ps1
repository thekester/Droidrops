#!/usr/bin/env pwsh

param(
    [string]$GitLabToken = $env:GITLAB_TOKEN,
    [string]$ForkProjectPath = 'theophileavenel/fdroiddata',
    [string]$UpstreamProjectPath = 'fdroid/fdroiddata',
    [string]$BranchPrefix = 'fdroid/com-droidrops-app',
    [string]$MetadataPath = (Join-Path (Join-Path $PSScriptRoot '..') 'build/fdroid/metadata/com.droidrops.app.yml'),
    [string]$ReleaseInfoPath = (Join-Path (Join-Path $PSScriptRoot '..') 'build/fdroid/release-info.txt'),
    [string]$RfpIssueNumber = '',
    [string]$FdroiddataIssueNumber = '',
    [switch]$BuildFirst = $true,
    [switch]$SkipMergeRequest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Net.Http -ErrorAction SilentlyContinue

function Get-HttpStatusCodeFromError {
    param([Parameter(Mandatory = $true)]$ErrorRecord)

    $exception = $ErrorRecord.Exception
    if ($null -eq $exception) {
        return $null
    }

    $responseProperty = $exception.PSObject.Properties['Response']
    if ($null -eq $responseProperty) {
        return $null
    }

    $response = $responseProperty.Value
    if ($null -eq $response) {
        return $null
    }

    $statusProperty = $response.PSObject.Properties['StatusCode']
    if ($null -eq $statusProperty -or $null -eq $statusProperty.Value) {
        return $null
    }

    return [int]$statusProperty.Value.value__
}

function Invoke-GitLabApi {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('GET', 'POST', 'PUT')]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [string]$Uri,

        [hashtable]$Body
    )

    $headers = @{ 'PRIVATE-TOKEN' = $GitLabToken }
    if ($PSBoundParameters.ContainsKey('Body') -and $null -ne $Body) {
        return Invoke-RestMethod -Method $Method -Headers $headers -ContentType 'application/json' -Uri $Uri -Body ($Body | ConvertTo-Json -Depth 10)
    }

    return Invoke-RestMethod -Method $Method -Headers $headers -Uri $Uri
}

function Get-GitLabProject {
    param([Parameter(Mandatory = $true)][string]$ProjectPath)

    $encoded = [uri]::EscapeDataString($ProjectPath)
    return Invoke-GitLabApi -Method GET -Uri "https://gitlab.com/api/v4/projects/$encoded"
}

function Get-GitLabBranch {
    param(
        [Parameter(Mandatory = $true)][int]$ProjectId,
        [Parameter(Mandatory = $true)][string]$BranchName
    )

    $encodedBranch = [uri]::EscapeDataString($BranchName)
    try {
        return Invoke-GitLabApi -Method GET -Uri "https://gitlab.com/api/v4/projects/$ProjectId/repository/branches/$encodedBranch"
    } catch {
        if ((Get-HttpStatusCodeFromError -ErrorRecord $_) -eq 404) {
            return $null
        }
        throw
    }
}

function New-GitLabBranch {
    param(
        [Parameter(Mandatory = $true)][int]$ProjectId,
        [Parameter(Mandatory = $true)][string]$BranchName,
        [Parameter(Mandatory = $true)][string]$RefBranch
    )

    Invoke-GitLabApi -Method POST -Uri "https://gitlab.com/api/v4/projects/$ProjectId/repository/branches" -Body @{
        branch = $BranchName
        ref    = $RefBranch
    } | Out-Null
}

function Set-GitLabRepositoryFile {
    param(
        [Parameter(Mandatory = $true)][int]$ProjectId,
        [Parameter(Mandatory = $true)][string]$BranchName,
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string]$Content,
        [Parameter(Mandatory = $true)][string]$CommitMessage
    )

    $encodedPath = [uri]::EscapeDataString($FilePath)
    $fileUri = "https://gitlab.com/api/v4/projects/$ProjectId/repository/files/$encodedPath"
    $fileReadUri = "${fileUri}?ref=$([uri]::EscapeDataString($BranchName))"

    $method = 'POST'
    try {
        Invoke-GitLabApi -Method GET -Uri $fileReadUri | Out-Null
        $method = 'PUT'
    } catch {
        if ((Get-HttpStatusCodeFromError -ErrorRecord $_) -ne 404) {
            throw
        }
    }

    Send-GitLabMultipartFileRequest -Method $method -Uri $fileUri -BranchName $BranchName -CommitMessage $CommitMessage -Content $Content
}

function Send-GitLabMultipartFileRequest {
    param(
        [Parameter(Mandatory = $true)][ValidateSet('POST', 'PUT')][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter(Mandatory = $true)][string]$BranchName,
        [Parameter(Mandatory = $true)][string]$CommitMessage,
        [Parameter(Mandatory = $true)][string]$Content
    )

    $client = [System.Net.Http.HttpClient]::new()
    try {
        $client.DefaultRequestHeaders.Add('PRIVATE-TOKEN', $GitLabToken)

        $multipart = [System.Net.Http.MultipartFormDataContent]::new()
        try {
            $branchContent = [System.Net.Http.StringContent]::new($BranchName, [System.Text.Encoding]::UTF8)
            $commitContent = [System.Net.Http.StringContent]::new($CommitMessage, [System.Text.Encoding]::UTF8)
            $fileContent = [System.Net.Http.StringContent]::new($Content, [System.Text.Encoding]::UTF8, 'text/plain')

            $multipart.Add($branchContent, 'branch')
            $multipart.Add($commitContent, 'commit_message')
            $multipart.Add($fileContent, 'content')

            switch ($Method) {
                'POST' { $response = $client.PostAsync($Uri, $multipart).GetAwaiter().GetResult() }
                'PUT' { $response = $client.PutAsync($Uri, $multipart).GetAwaiter().GetResult() }
            }

            $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if (-not $response.IsSuccessStatusCode) {
                throw "GitLab file $Method failed ($([int]$response.StatusCode)): $responseBody"
            }
        } finally {
            $multipart.Dispose()
            $branchContent.Dispose()
            $commitContent.Dispose()
            $fileContent.Dispose()
        }
    } finally {
        $client.Dispose()
    }
}

function Get-OpenMergeRequest {
    param(
        [Parameter(Mandatory = $true)][int]$TargetProjectId,
        [Parameter(Mandatory = $true)][int]$SourceProjectId,
        [Parameter(Mandatory = $true)][string]$SourceBranch,
        [Parameter(Mandatory = $true)][string]$TargetBranch
    )

    $query = "https://gitlab.com/api/v4/projects/$TargetProjectId/merge_requests?state=opened&source_branch=$([uri]::EscapeDataString($SourceBranch))&target_branch=$([uri]::EscapeDataString($TargetBranch))"
    $mrs = @(Invoke-GitLabApi -Method GET -Uri $query)
    return $mrs | Where-Object {
        $_.PSObject.Properties['source_project_id'] -and $_.source_project_id -eq $SourceProjectId
    } | Select-Object -First 1
}

function New-MergeRequest {
    param(
        [Parameter(Mandatory = $true)][int]$TargetProjectId,
        [Parameter(Mandatory = $true)][int]$SourceProjectId,
        [Parameter(Mandatory = $true)][string]$SourceBranch,
        [Parameter(Mandatory = $true)][string]$TargetBranch,
        [Parameter(Mandatory = $true)][string]$Title,
        [Parameter(Mandatory = $true)][string]$Description
    )

    return Invoke-GitLabApi -Method POST -Uri "https://gitlab.com/api/v4/projects/$TargetProjectId/merge_requests" -Body @{
        source_project_id = $SourceProjectId
        source_branch     = $SourceBranch
        target_branch     = $TargetBranch
        title             = $Title
        description       = $Description
        remove_source_branch = $false
    }
}

function Get-FdroidMergeRequestBody {
    param(
        [Parameter(Mandatory = $true)][string]$AppName,
        [Parameter(Mandatory = $true)][string]$VersionName,
        [Parameter(Mandatory = $true)][string]$VersionTag,
        [Parameter(Mandatory = $true)][string]$ReleaseInfo,
        [Parameter(Mandatory = $true)][string]$RfpIssueNumber,
        [Parameter(Mandatory = $true)][string]$FdroiddataIssueNumber
    )

    $lines = @(
        '## Required'
        ''
        '- [ ] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy)'
        '- [ ] The original app author has been notified (and does not oppose the inclusion)'
        '- [ ] All related [fdroiddata](https://gitlab.com/fdroid/fdroiddata/issues) and [RFP issues](https://gitlab.com/fdroid/rfp/issues) have been referenced in this merge request'
        '- [ ] Builds with `fdroid build` and all pipelines pass'
        '- [ ] There is an issue tracker and contact info of the author so that we can report bugs and contact the author.'
        ''
        '## Strongly Recommended'
        ''
        '- [x] The upstream app source code repo contains the app metadata in a [Fastlane](https://gitlab.com/snippets/1895688) or [Triple-T](https://gitlab.com/snippets/1901490) folder structure'
        '- [x] Releases are tagged and auto update is enabled'
        ''
        '## Suggested'
        ''
        '- [ ] External repos are added as git submodules instead of srclibs'
        '- [ ] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds)'
        '- [ ] Multiple apks for native code'
        ''
        '## App'
        ''
        "- App name: $AppName"
        '- Package name: `com.droidrops.app`'
        "- Version: $VersionName"
        "- Tag: $VersionTag"
        ''
        '## Release summary'
        ''
        '```text'
        $ReleaseInfo
        '```'
        ''
        'This branch only contains the metadata file for fdroiddata.'
    )

    if (-not [string]::IsNullOrWhiteSpace($RfpIssueNumber) -or -not [string]::IsNullOrWhiteSpace($FdroiddataIssueNumber)) {
        $lines[4] = '- [x] All related [fdroiddata](https://gitlab.com/fdroid/fdroiddata/issues) and [RFP issues](https://gitlab.com/fdroid/rfp/issues) have been referenced in this merge request'
    }

    if (-not [string]::IsNullOrWhiteSpace($RfpIssueNumber)) {
        $lines += ''
        $lines += "Closes rfp#$RfpIssueNumber"
    }

    if (-not [string]::IsNullOrWhiteSpace($FdroiddataIssueNumber)) {
        $lines += ''
        $lines += "Closes fdroiddata#$FdroiddataIssueNumber"
    }

    return ($lines -join [Environment]::NewLine)
}

if ([string]::IsNullOrWhiteSpace($GitLabToken)) {
    throw 'Set GITLAB_TOKEN or pass -GitLabToken.'
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

if ($BuildFirst) {
    & (Join-Path $PSScriptRoot 'prepare-fdroid-release.ps1')
}

Write-Host 'Stage: load metadata'
if (-not (Test-Path $MetadataPath)) {
    throw "Metadata file not found: $MetadataPath"
}

if (-not (Test-Path $ReleaseInfoPath)) {
    throw "Release info file not found: $ReleaseInfoPath"
}

$buildFile = Get-Content -Raw -Path (Join-Path $repoRoot 'app/build.gradle.kts')
$versionName = [regex]::Match($buildFile, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if ([string]::IsNullOrWhiteSpace($versionName)) {
    throw 'Unable to determine versionName from app/build.gradle.kts.'
}

$versionTag = "v$versionName"
$branchName = "$BranchPrefix-$versionName"
$metadataFileName = Split-Path -Leaf $MetadataPath

Write-Host 'Stage: resolve projects'
$forkProject = Get-GitLabProject -ProjectPath $ForkProjectPath
$upstreamProject = Get-GitLabProject -ProjectPath $UpstreamProjectPath

Write-Host "Stage: ensure branch $branchName"
$existingBranch = Get-GitLabBranch -ProjectId $forkProject.id -BranchName $branchName
if ($null -eq $existingBranch) {
    $defaultBranch = $forkProject.default_branch
    if ([string]::IsNullOrWhiteSpace($defaultBranch)) {
        $defaultBranch = $upstreamProject.default_branch
    }
    if ([string]::IsNullOrWhiteSpace($defaultBranch)) {
        $defaultBranch = 'master'
    }

    New-GitLabBranch -ProjectId $forkProject.id -BranchName $branchName -RefBranch $defaultBranch
} elseif ($existingBranch.protected) {
    throw "Branch $branchName is protected in $ForkProjectPath. Unprotect it before opening the merge request."
}

Write-Host 'Stage: upload metadata'
$metadataContent = Get-Content -Raw -Path $MetadataPath
$releaseInfo = Get-Content -Raw -Path $ReleaseInfoPath
$metadataCommitMessage = "Add F-Droid metadata for Droidrops $versionName"
Set-GitLabRepositoryFile -ProjectId $forkProject.id -BranchName $branchName -FilePath "metadata/$metadataFileName" -Content $metadataContent -CommitMessage $metadataCommitMessage

$mrUrl = $null
if (-not $SkipMergeRequest) {
    Write-Host 'Stage: merge request'
    $title = 'New app: Droidrops'
    $description = Get-FdroidMergeRequestBody -AppName 'Droidrops' -VersionName $versionName -VersionTag $versionTag -ReleaseInfo $releaseInfo -RfpIssueNumber $RfpIssueNumber -FdroiddataIssueNumber $FdroiddataIssueNumber

    $existingMr = Get-OpenMergeRequest -TargetProjectId $upstreamProject.id -SourceProjectId $forkProject.id -SourceBranch $branchName -TargetBranch $upstreamProject.default_branch
    if ($existingMr) {
        $mrUrl = $existingMr.web_url
    } else {
        try {
            $mr = New-MergeRequest -TargetProjectId $upstreamProject.id -SourceProjectId $forkProject.id -SourceBranch $branchName -TargetBranch $upstreamProject.default_branch -Title $title -Description $description
            $mrUrl = $mr.web_url
        } catch {
            if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 403) {
                $mrUrl = "https://gitlab.com/$ForkProjectPath/-/merge_requests/new?merge_request%5Bsource_branch%5D=$([uri]::EscapeDataString($branchName))&merge_request%5Btarget_project_id%5D=$($upstreamProject.id)&merge_request%5Btarget_branch%5D=$([uri]::EscapeDataString($upstreamProject.default_branch))"
            } else {
                throw
            }
        }
    }
}

Write-Host "Fork project:   $($forkProject.path_with_namespace)"
Write-Host "Upstream repo:  $($upstreamProject.path_with_namespace)"
Write-Host "Branch:         $branchName"
Write-Host "Metadata file:  metadata/$metadataFileName"
Write-Host "Version tag:    $versionTag"
if ($mrUrl) {
    Write-Host "Merge request:  $mrUrl"
} else {
    Write-Host "Merge request:  skipped"
}
