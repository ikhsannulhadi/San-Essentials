[CmdletBinding()]
param(
    [string[]] $MinecraftVersion = @('1.21.1'),
    [ValidateSet('neoforge')]
    [string[]] $Loader = @('neoforge'),
    [switch] $Clean,
    [switch] $ContinueOnError,
    [string[]] $GradleArgs = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$propertiesPath = Join-Path $scriptRoot 'gradle.properties'
$versionsRoot = Join-Path $scriptRoot 'src\versions'
$libsDir = Join-Path $scriptRoot 'build\libs'
$isWindowsHost = $env:OS -eq 'Windows_NT'
$gradleWrapper = Join-Path $scriptRoot ($(if ($isWindowsHost) { 'gradlew.bat' } else { 'gradlew' }))

if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Gradle wrapper was not found: $gradleWrapper"
}

function Read-GradleProperties {
    param([Parameter(Mandatory = $true)][string] $Path)

    $properties = [ordered]@{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -le 0) {
            continue
        }

        $key = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        $properties[$key] = $value
    }

    return $properties
}

function Get-NextPatchVersion {
    param([Parameter(Mandatory = $true)][version] $Version)

    $build = if ($Version.Build -lt 0) { 0 } else { $Version.Build }
    return New-Object System.Version -ArgumentList $Version.Major, $Version.Minor, ($build + 1)
}

function Get-HighestBuiltVersion {
    param(
        [Parameter(Mandatory = $true)][string] $Directory,
        [Parameter(Mandatory = $true)][string] $Prefix
    )

    if (-not (Test-Path -LiteralPath $Directory)) {
        return $null
    }

    $highest = $null
    foreach ($jar in Get-ChildItem -LiteralPath $Directory -Filter "$Prefix-*.jar" -File) {
        if ($jar.Name -match "^$([regex]::Escape($Prefix))-(?:.*-)?(\d+\.\d+\.\d+)\.jar$") {
            $candidate = [version]$matches[1]
            if ($null -eq $highest -or $candidate -gt $highest) {
                $highest = $candidate
            }
        }
    }

    return $highest
}

function Set-GradleProperty {
    param(
        [Parameter(Mandatory = $true)][string] $Path,
        [Parameter(Mandatory = $true)][string] $Key,
        [Parameter(Mandatory = $true)][string] $Value
    )

    $lines = @(Get-Content -LiteralPath $Path)
    $pattern = '^\s*' + [regex]::Escape($Key) + '\s*='
    $updated = $false
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match $pattern) {
            $lines[$index] = "$Key=$Value"
            $updated = $true
            break
        }
    }

    if (-not $updated) {
        $lines += "$Key=$Value"
    }

    [System.IO.File]::WriteAllLines($Path, $lines, [System.Text.UTF8Encoding]::new($false))
}

function Add-Version {
    param(
        [Parameter(Mandatory = $true)][System.Collections.Specialized.OrderedDictionary] $Set,
        [Parameter(Mandatory = $true)][string] $Version
    )

    if (-not $Set.Contains($Version)) {
        $Set[$Version] = $true
    }
}

$properties = Read-GradleProperties -Path $propertiesPath
$discoveredVersions = [ordered]@{}
$requestedMinecraftVersions = @($MinecraftVersion)
$requestedLoaders = @($Loader)

if (-not $properties.Contains('mod_id')) {
    throw 'The mod_id property is required in gradle.properties.'
}

$modId = $properties['mod_id']

if ($properties.Contains('minecraft_version')) {
    Add-Version -Set $discoveredVersions -Version $properties['minecraft_version']
}

foreach ($key in $properties.Keys) {
    if ($key -match '^mc_(.+?)_(neo_version|neo_version_range|fabric_api_version|fabric_loader_version|target_java_version|parchment_minecraft_version|parchment_mappings_version|minecraft_version_range|fabric_minecraft_version_range|loader_version_range)$') {
        Add-Version -Set $discoveredVersions -Version ($matches[1] -replace '_', '.')
    }
}

if (Test-Path -LiteralPath $versionsRoot) {
    foreach ($directory in Get-ChildItem -LiteralPath $versionsRoot -Directory) {
        Add-Version -Set $discoveredVersions -Version $directory.Name
    }
}

$versions = @(
    if ($requestedMinecraftVersions.Count -gt 0) {
        $requestedMinecraftVersions
    } else {
        $discoveredVersions.Keys
    }
)

if ($versions.Count -eq 0) {
    throw 'No Minecraft versions were found. Pass -MinecraftVersion or add versions to gradle.properties/src/versions.'
}

if (-not $properties.Contains('mod_version')) {
    throw 'The mod_version property is required in gradle.properties.'
}

$currentVersion = [version]$properties['mod_version']
$highestBuiltVersion = Get-HighestBuiltVersion -Directory $libsDir -Prefix "$modId-neoforge"
if ($null -ne $highestBuiltVersion -and $highestBuiltVersion -ge $currentVersion) {
    $targetVersion = Get-NextPatchVersion -Version $highestBuiltVersion
} else {
    $targetVersion = $currentVersion
}

Write-Host "==> Version: $currentVersion -> $targetVersion"

$taskByLoader = @{
    neoforge = 'buildNeoForge'
}
$baseGradleArgs = @('--no-daemon', '--no-configuration-cache') + @($GradleArgs)
$failures = New-Object System.Collections.Generic.List[object]

Push-Location $scriptRoot
try {
    if ($Clean) {
        Write-Host '==> Cleaning build outputs'
        & $gradleWrapper 'clean' @baseGradleArgs
        if ($LASTEXITCODE -ne 0) {
            exit $LASTEXITCODE
        }
    }

    foreach ($version in $versions) {
        foreach ($loaderName in $requestedLoaders) {
            $normalizedLoader = $loaderName.ToLowerInvariant()
            $taskName = $taskByLoader[$normalizedLoader]
            $gradleCommandArgs = @("-Ptarget_minecraft_version=$version", "-Pmod_version=$targetVersion", $taskName) + $baseGradleArgs

            Write-Host "==> Building $normalizedLoader for Minecraft $version"
            & $gradleWrapper @gradleCommandArgs
            $exitCode = $LASTEXITCODE

            if ($exitCode -ne 0) {
                $failures.Add([pscustomobject]@{
                    Loader = $normalizedLoader
                    MinecraftVersion = $version
                    ExitCode = $exitCode
                })

                if (-not $ContinueOnError) {
                    Write-Error "Build failed for $normalizedLoader Minecraft $version with exit code $exitCode."
                    exit $exitCode
                }
            }
        }
    }
}
finally {
    Pop-Location
}

if ($failures.Count -gt 0) {
    Write-Host ''
    Write-Host 'Build failures:'
    foreach ($failure in $failures) {
        Write-Host " - $($failure.Loader) $($failure.MinecraftVersion): exit code $($failure.ExitCode)"
    }
    exit 1
}

if ($targetVersion -ne $currentVersion) {
    Set-GradleProperty -Path $propertiesPath -Key 'mod_version' -Value $targetVersion.ToString()
    Write-Host "==> Updated mod_version to $targetVersion"
}

Write-Host ''
Write-Host 'All requested builds completed.'

if (Test-Path -LiteralPath $libsDir) {
    Write-Host "Jars in ${libsDir}:"
    Get-ChildItem -LiteralPath $libsDir -Filter "$modId-*.jar" |
        Sort-Object Name |
        ForEach-Object { Write-Host " - $($_.Name)" }
}
