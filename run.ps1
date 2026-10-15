param(
    [ValidateSet('play', 'test', 'verify', 'ui-test')]
    [string]$Task = 'play',
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$projectRoot = $PSScriptRoot
$mavenVersion = '3.9.9'
$mavenDirectory = Join-Path $projectRoot ".tools/apache-maven-$mavenVersion"

# Find a cozy JDK, including the one bundled with VS Code (˘ᵕ˘)
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$javacCommand = Get-Command javac -ErrorAction SilentlyContinue
if ($javacCommand) { $candidates += Split-Path (Split-Path $javacCommand.Source -Parent) -Parent }
$candidates += Get-ChildItem -Path "$env:USERPROFILE/.vscode/extensions/redhat.java-*/jre/*/bin/javac.exe" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    ForEach-Object { Split-Path (Split-Path $_.FullName -Parent) -Parent }

$jdkDirectory = $null
foreach ($candidate in $candidates) {
    $releaseFile = Join-Path $candidate 'release'
    if (Test-Path -LiteralPath $releaseFile) {
        $versionLine = Get-Content -LiteralPath $releaseFile | Where-Object { $_ -match '^JAVA_VERSION=' }
        if ($versionLine -match 'JAVA_VERSION="(\d+)' -and [int]$Matches[1] -ge 25) {
            $jdkDirectory = $candidate
            break
        }
    }
}
if (-not $jdkDirectory) {
    throw 'Java 25 or later is required. Set JAVA_HOME to your JDK or install the Java Extension Pack in VS Code.'
}

if (-not (Test-Path -LiteralPath (Join-Path $mavenDirectory 'bin/m2.conf'))) {
    if ($Offline) { throw 'Maven is not installed yet. Run once with an internet connection first.' }
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $toolsDirectory = Join-Path $projectRoot '.tools'
    New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
    $archive = Join-Path $toolsDirectory "apache-maven-$mavenVersion-bin.zip"
    $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
    Write-Host 'Setting up Maven for this project ...'
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $archive -TimeoutSec 120
    $checksum = (Invoke-WebRequest -UseBasicParsing -Uri ($url + '.sha512') -TimeoutSec 30).Content.Trim().Split(' ')[0]
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash -ine $checksum) {
        throw 'The Maven checksum does not match. Please try downloading it again.'
    }
    Expand-Archive -LiteralPath $archive -DestinationPath $toolsDirectory -Force
}

# Launch Maven through Java so network folders feel at home, too (•ᴗ•)
$classworlds = (Get-ChildItem -LiteralPath (Join-Path $mavenDirectory 'boot') -Filter 'plexus-classworlds-*.jar' |
    Select-Object -First 1).FullName
$mavenArguments = @('--batch-mode', '--no-transfer-progress', '--file', (Join-Path $projectRoot 'pom.xml'))
if ($Offline) { $mavenArguments += '--offline' }
switch ($Task) {
    'play' { $mavenArguments += @('compile', 'javafx:run') }
    'test' { $mavenArguments += 'test' }
    'verify' { $mavenArguments += 'verify' }
    'ui-test' { $mavenArguments += @('test', '-Ddino.uiTests=true') }
}

$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $jdkDirectory
    & (Join-Path $jdkDirectory 'bin/java.exe') '--enable-native-access=ALL-UNNAMED' `
        "-Dmaven.home=$mavenDirectory" "-Dmaven.multiModuleProjectDirectory=$projectRoot" `
        "-Dclassworlds.conf=$mavenDirectory/bin/m2.conf" "-Dmaven.repo.local=$projectRoot/.tools/repository" `
        -classpath $classworlds org.codehaus.plexus.classworlds.launcher.Launcher @mavenArguments
    $result = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
exit $result
