[CmdletBinding()]
param(
    [string]$PackageName = "com.voxia.assistant",
    [string]$ApkPath = "app/build/outputs/apk/debug/app-debug.apk",
    [string]$OutputRoot = "evaluation/field/reports",
    [switch]$InstallApk,
    [switch]$Launch,
    [switch]$CollectLogcat,
    [switch]$GrantRuntimePermissions,
    [switch]$ListOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-Adb {
    $fromPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    $candidates = @()
    if ($env:ANDROID_HOME) {
        $candidates += Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
    }
    $candidates += Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "adb introuvable. Installe Android Platform Tools ou ajoute adb au PATH."
}

function Invoke-Adb {
    param(
        [string]$Adb,
        [string[]]$Arguments,
        [string]$OutFile
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $Adb @Arguments 2>&1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($OutFile) {
        $output | ForEach-Object { $_.ToString() } | Out-File -FilePath $OutFile -Encoding utf8
    }
    if ($LASTEXITCODE -ne 0) {
        throw "adb a échoué: $($Arguments -join ' ')"
    }
    return $output
}

$adb = Resolve-Adb
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path $OutputRoot $timestamp
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Invoke-Adb -Adb $adb -Arguments @("start-server") -OutFile (Join-Path $outDir "adb_start_server.txt") | Out-Null
$devices = Invoke-Adb -Adb $adb -Arguments @("devices", "-l") -OutFile (Join-Path $outDir "adb_devices.txt")

if ($ListOnly) {
    Write-Output "adb: $adb"
    $devices | ForEach-Object { Write-Output $_ }
    Write-Output "Rapport: $outDir"
    exit 0
}

$deviceLines = @($devices | Where-Object { $_ -match "\sdevice\s" -and $_ -notmatch "^List of devices" })
if ($deviceLines.Count -ne 1) {
    throw "Branche exactement un téléphone autorisé. Appareils détectés: $($deviceLines.Count). Voir $outDir\adb_devices.txt"
}

$serial = ($deviceLines[0] -split "\s+")[0]

function Get-DeviceProp {
    param([string]$Name)
    $value = & $adb -s $serial shell getprop $Name 2>$null
    return ($value -join "").Trim()
}

$gitCommit = ""
try {
    $gitCommit = ((& git rev-parse --short HEAD 2>$null) -join "").Trim()
} catch {
    $gitCommit = "unknown"
}

$context = [ordered]@{
    created_at = (Get-Date).ToString("o")
    git_commit = $gitCommit
    adb = $adb
    serial = $serial
    package_name = $PackageName
    manufacturer = Get-DeviceProp "ro.product.manufacturer"
    model = Get-DeviceProp "ro.product.model"
    device = Get-DeviceProp "ro.product.device"
    android_release = Get-DeviceProp "ro.build.version.release"
    android_sdk = Get-DeviceProp "ro.build.version.sdk"
    abi = Get-DeviceProp "ro.product.cpu.abi"
}
$context | ConvertTo-Json -Depth 3 | Out-File -FilePath (Join-Path $outDir "device_context.json") -Encoding utf8

if ($InstallApk) {
    if (-not (Test-Path $ApkPath)) {
        throw "APK introuvable: $ApkPath. Lance d'abord .\gradlew.bat assembleDebug."
    }
    Invoke-Adb -Adb $adb -Arguments @("-s", $serial, "install", "-r", $ApkPath) -OutFile (Join-Path $outDir "adb_install.txt") | Out-Null
}

$packageCheck = & $adb -s $serial shell pm path $PackageName 2>&1
$packageCheck | Out-File -FilePath (Join-Path $outDir "package_path.txt") -Encoding utf8

if ($GrantRuntimePermissions) {
    $permissions = @(
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.POST_NOTIFICATIONS"
    )
    foreach ($permission in $permissions) {
        (& $adb -s $serial shell pm grant $PackageName $permission 2>&1) |
            Out-File -FilePath (Join-Path $outDir "grant_$($permission.Split('.')[-1]).txt") -Encoding utf8
    }
}

if ($CollectLogcat) {
    Invoke-Adb -Adb $adb -Arguments @("-s", $serial, "logcat", "-c") -OutFile (Join-Path $outDir "logcat_clear.txt") | Out-Null
}

if ($Launch) {
    Invoke-Adb -Adb $adb -Arguments @("-s", $serial, "shell", "monkey", "-p", $PackageName, "-c", "android.intent.category.LAUNCHER", "1") -OutFile (Join-Path $outDir "launch.txt") | Out-Null
}

if ($CollectLogcat) {
    Start-Sleep -Seconds 5
    Invoke-Adb -Adb $adb -Arguments @("-s", $serial, "logcat", "-d", "-v", "time", "-t", "1000") -OutFile (Join-Path $outDir "logcat_tail.txt") | Out-Null
}

Write-Output "Smoke téléphone terminé."
Write-Output "Rapport local: $outDir"
