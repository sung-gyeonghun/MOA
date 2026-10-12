param([string]$BuildRoot = 'G:\moa-build', [switch]$VerifyDevice, [switch]$ReleaseArtifacts)
$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BuildPath = [IO.Path]::GetFullPath($BuildRoot)
if ($BuildPath -eq [IO.Path]::GetPathRoot($BuildPath) -or $BuildPath -eq $ProjectRoot -or $ProjectRoot.StartsWith($BuildPath.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase) -or $BuildPath -match '[^\x00-\x7F]') { throw 'Use a dedicated ASCII build directory outside the project ancestors.' }
New-Item -ItemType Directory -Force -Path $BuildPath | Out-Null
foreach ($Name in @('gradle','settings.gradle.kts','build.gradle.kts','gradle.properties','gradlew','gradlew.bat','local.properties')) {
    $Source = Join-Path $ProjectRoot $Name
    if (Test-Path -LiteralPath $Source) { Copy-Item -LiteralPath $Source -Destination $BuildPath -Recurse -Force }
}
New-Item -ItemType Directory -Force -Path (Join-Path $BuildPath 'app') | Out-Null
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'app\src'), (Join-Path $ProjectRoot 'app\build.gradle.kts') -Destination (Join-Path $BuildPath 'app') -Recurse -Force
$GradleTasks = @(':app:assembleDebug', ':app:assemblePreview', ':app:testDebugUnitTest', ':app:lintDebug', ':app:lintPreview')
if ($VerifyDevice) { $GradleTasks += ':app:connectedDebugAndroidTest' }
if ($ReleaseArtifacts) { $GradleTasks += @(':app:assembleRelease', ':app:bundleRelease') }
& (Join-Path $BuildPath 'gradlew.bat') -p $BuildPath @GradleTasks
if ($LASTEXITCODE -ne 0) { throw 'Build or verification failed.' }
$Dist = Join-Path $ProjectRoot 'dist'
New-Item -ItemType Directory -Force -Path $Dist | Out-Null
Copy-Item -LiteralPath (Join-Path $BuildPath 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $Dist 'moa-debug.apk') -Force
Copy-Item -LiteralPath (Join-Path $BuildPath 'app\build\outputs\apk\preview\app-preview.apk') -Destination (Join-Path $Dist 'moa-preview.apk') -Force
if ($ReleaseArtifacts) {
    Copy-Item -LiteralPath (Join-Path $BuildPath 'app\build\outputs\apk\release\app-release-unsigned.apk') -Destination (Join-Path $Dist 'moa-release-unsigned.apk') -Force
    Copy-Item -LiteralPath (Join-Path $BuildPath 'app\build\outputs\bundle\release\app-release.aab') -Destination (Join-Path $Dist 'moa-release-unsigned.aab') -Force
}
