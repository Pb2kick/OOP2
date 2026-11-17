# AMBOT build script - plain JDK tools, no Maven/Gradle.
# Usage (from the project folder):  .\build.cmd <task> [-Select fully.qualified.TestClass]
#   deps     download the JUnit console runner into lib\ (tests only, never shipped)
#   compile  compile src\ into build\classes and copy resources\
#   test     compile and run the JUnit tests in test\
#   run      compile and start the game
#   clean    delete build\
param(
    [ValidateSet('deps', 'compile', 'test', 'run', 'clean')]
    [string]$Task = 'run',
    [string]$Select
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$MainClass = 'ambot.Main'
$JunitVersion = '1.11.4'
$JunitJar = "lib\junit-platform-console-standalone-$JunitVersion.jar"
$JunitUrl = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$JunitVersion/junit-platform-console-standalone-$JunitVersion.jar"

$Jdk = $env:AMBOT_JDK
if (-not $Jdk) { $Jdk = 'C:\jdk-26' }
$Javac = Join-Path $Jdk 'bin\javac.exe'
$Java = Join-Path $Jdk 'bin\java.exe'
if (-not (Test-Path -LiteralPath $Javac)) {
    throw "JDK not found at '$Jdk'. Install JDK 21+ or set AMBOT_JDK to its folder."
}

function Invoke-Checked([string]$exe, [string[]]$arguments) {
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function Get-Deps {
    if (Test-Path -LiteralPath $JunitJar) { return }
    New-Item -ItemType Directory -Force lib | Out-Null
    Write-Host "Downloading JUnit console runner $JunitVersion ..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $wc = New-Object Net.WebClient
    $wc.DownloadFile($JunitUrl, (Join-Path $PSScriptRoot $JunitJar))
    $expected = ($wc.DownloadString("$JunitUrl.sha1")).Trim().Split(' ')[0].ToLower()
    $actual = (Get-FileHash -Algorithm SHA1 -LiteralPath $JunitJar).Hash.ToLower()
    if ($expected -ne $actual) {
        Remove-Item -LiteralPath $JunitJar
        throw "SHA-1 mismatch for $JunitJar (expected $expected, got $actual)"
    }
    Write-Host "OK: $JunitJar (sha1 $actual)"
}

function Write-SourceList([string]$dir, [string]$listFile) {
    if (-not (Test-Path -LiteralPath $dir)) { return 0 }
    $files = @(Get-ChildItem -LiteralPath $dir -Recurse -Filter *.java | ForEach-Object { '"' + ($_.FullName -replace '\\', '/') + '"' })
    [IO.File]::WriteAllLines((Join-Path $PSScriptRoot $listFile), [string[]]$files)
    return $files.Count
}

function Invoke-Compile {
    New-Item -ItemType Directory -Force build\classes | Out-Null
    [void](Write-SourceList 'src' 'build\sources.txt')
    Invoke-Checked $Javac @('--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-d', 'build\classes', '@build\sources.txt')
    Copy-Item -Path 'resources\*' -Destination 'build\classes' -Recurse -Force
}

function Invoke-Tests {
    Get-Deps
    Invoke-Compile
    New-Item -ItemType Directory -Force build\test-classes | Out-Null
    $count = Write-SourceList 'test' 'build\test-sources.txt'
    if ($count -eq 0) { Write-Host 'No tests found.'; return }
    Invoke-Checked $Javac @('--release', '21', '-encoding', 'UTF-8', '-cp', "build\classes;$JunitJar", '-d', 'build\test-classes', '@build\test-sources.txt')
    $runArgs = @('-jar', $JunitJar, 'execute', '--disable-banner', '--details=tree', '--class-path', 'build\classes;build\test-classes')
    if ($Select) { $runArgs += @('--select-class', $Select) } else { $runArgs += '--scan-class-path' }
    Invoke-Checked $Java $runArgs
}

switch ($Task) {
    'deps'    { Get-Deps }
    'compile' { Invoke-Compile }
    'test'    { Invoke-Tests }
    'run'     { Invoke-Compile; Invoke-Checked $Java @('-cp', 'build\classes', $MainClass) }
    'clean'   { if (Test-Path build) { Remove-Item -Recurse -Force build } }
}
