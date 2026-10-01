param(
    [string]$Sdk = "$env:LOCALAPPDATA/Android/Sdk"
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$buildTools = Join-Path $Sdk 'build-tools/36.0.0'
$androidJar = Join-Path $Sdk 'platforms/android-34/android.jar'

function Invoke-BuildTool {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Ha fallat $Command ($LASTEXITCODE)" }
}

New-Item -ItemType Directory -Force build/windows/classes, build/windows/dex | Out-Null
Copy-Item -LiteralPath $androidJar -Destination build/windows/android.jar -Force
$androidJar = 'build/windows/android.jar'
$sources = @(Get-ChildItem src -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
Invoke-BuildTool 'javac' (@('--release', '8', '-encoding', 'UTF-8', '-cp', $androidJar, '-d', 'build/windows/classes') + $sources)
$classes = @(Get-ChildItem build/windows/classes -Filter '*.class' -Recurse | ForEach-Object { $_.FullName })
Invoke-BuildTool "$buildTools/d8.bat" (@('--min-api', '23', '--lib', $androidJar, '--output', 'build/windows/dex') + $classes)
Invoke-BuildTool "$buildTools/aapt.exe" @('package', '-f', '-M', 'AndroidManifest.xml', '-I', $androidJar, '-F', 'build/windows/app.unsigned.apk')
Push-Location build/windows/dex
try {
    Invoke-BuildTool "$buildTools/aapt.exe" @('add', '../app.unsigned.apk', 'classes.dex')
} finally {
    Pop-Location
}
Invoke-BuildTool "$buildTools/zipalign.exe" @('-f', '4', 'build/windows/app.unsigned.apk', 'build/windows/app.aligned.apk')
if (-not (Test-Path etiqueta.keystore)) {
    $keytool = 'keytool'
    if (-not (Get-Command $keytool -ErrorAction SilentlyContinue)) {
        $ErrorActionPreference = 'Continue'
        $javaSettings = & java -XshowSettings:properties -version 2>&1
        $ErrorActionPreference = 'Stop'
        $javaDirectory = ($javaSettings | Select-String '^\s*java.home = (.+)$').Matches.Groups[1].Value
        $keytool = Join-Path $javaDirectory 'bin/keytool.exe'
    }
    Invoke-BuildTool $keytool @('-genkeypair', '-keystore', 'etiqueta.keystore', '-storepass', 'etiqueta', '-keypass', 'etiqueta', '-alias', 'etiqueta', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-dname', 'CN=Etiqueta Zebra')
}
Invoke-BuildTool "$buildTools/apksigner.bat" @('sign', '--ks', 'etiqueta.keystore', '--ks-pass', 'pass:etiqueta', '--key-pass', 'pass:etiqueta', '--out', 'EtiquetaZebra.apk', 'build/windows/app.aligned.apk')
Invoke-BuildTool "$buildTools/apksigner.bat" @('verify', '--verbose', 'EtiquetaZebra.apk')
