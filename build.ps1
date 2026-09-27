param([string]$JdkPath, [string]$SdkPath)
$ErrorActionPreference = 'Stop'
$sourceRoot = $PSScriptRoot
$workspaceRoot = [System.IO.Path]::GetFullPath((Join-Path $sourceRoot '..\..'))
if (!$JdkPath) { $JdkPath = (Get-ChildItem -LiteralPath (Join-Path $workspaceRoot 'work\toolchain\java') -Directory | Select-Object -First 1).FullName }
if (!$SdkPath) { $SdkPath = Join-Path $workspaceRoot 'work\toolchain\sdk' }
$buildRoot = Join-Path $workspaceRoot 'work\android-build'
$classes = Join-Path $buildRoot 'classes'
$generated = Join-Path $buildRoot 'generated'
$dex = Join-Path $buildRoot 'dex'
$testClasses = Join-Path $buildRoot 'test-classes'
foreach ($dir in @($buildRoot,$classes,$generated,$dex,$testClasses)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
$java = Join-Path $JdkPath 'bin\java.exe'
$javac = Join-Path $JdkPath 'bin\javac.exe'
$jar = Join-Path $JdkPath 'bin\jar.exe'
$keytool = Join-Path $JdkPath 'bin\keytool.exe'
$androidJar = Join-Path $SdkPath 'platforms\android-35\android.jar'
$buildTools = Join-Path $SdkPath 'build-tools\35.0.0'
$aapt = Join-Path $buildTools 'aapt2.exe'
function CheckExit([string]$stage) { if ($LASTEXITCODE -ne 0) { throw "$stage failed with exit code $LASTEXITCODE" } }
Write-Output 'Testing timer state machine'
& $javac -encoding UTF-8 -d $testClasses (Join-Path $sourceRoot 'app\src\main\java\com\focus\personal\TimerEngine.java') (Join-Path $sourceRoot 'tests\TimerEngineTest.java')
CheckExit 'Compile timer tests'
& $java -cp $testClasses TimerEngineTest
CheckExit 'Timer tests'
& node (Join-Path $sourceRoot 'tests\model.test.cjs')
CheckExit 'Model tests'
Write-Output 'Compiling Android resources'
$resourceZip = Join-Path $buildRoot 'resources.zip'
& $aapt compile --dir (Join-Path $sourceRoot 'app\src\main\res') -o $resourceZip
CheckExit 'Resource compile'
$baseApk = Join-Path $buildRoot 'base.apk'
& $aapt link -o $baseApk -I $androidJar --manifest (Join-Path $sourceRoot 'app\src\main\AndroidManifest.xml') --java $generated -A (Join-Path $sourceRoot 'app\src\main\assets') --min-sdk-version 26 --target-sdk-version 35 $resourceZip
CheckExit 'Resource link'
Write-Output 'Compiling Android app'
$javaFiles = @((Get-ChildItem -LiteralPath (Join-Path $sourceRoot 'app\src\main\java') -Recurse -Filter '*.java').FullName) + @((Get-ChildItem -LiteralPath $generated -Recurse -Filter '*.java').FullName)
& $javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -classpath $androidJar -d $classes $javaFiles
CheckExit 'Java compile'
$classesJar = Join-Path $buildRoot 'classes.jar'
& $jar cf $classesJar -C $classes .
CheckExit 'Class archive'
& $java -cp (Join-Path $buildTools 'lib\d8.jar') com.android.tools.r8.D8 --lib $androidJar --min-api 26 --output $dex $classesJar
CheckExit 'Dex compile'
Copy-Item -LiteralPath $baseApk -Destination (Join-Path $buildRoot 'unsigned.apk') -Force
& $jar uf (Join-Path $buildRoot 'unsigned.apk') -C $dex classes.dex
CheckExit 'APK package'
$aligned = Join-Path $buildRoot 'aligned.apk'
& (Join-Path $buildTools 'zipalign.exe') -f -p 4 (Join-Path $buildRoot 'unsigned.apk') $aligned
CheckExit 'ZIP alignment'
$signing = Join-Path $workspaceRoot 'work\signing'
New-Item -ItemType Directory -Path $signing -Force | Out-Null
$keyStore = Join-Path $signing 'luma.jks'
if (!(Test-Path -LiteralPath $keyStore)) {
    & $keytool -genkeypair -keystore $keyStore -storepass focus-personal-local -keypass focus-personal-local -alias personal -dname 'CN=Luma, OU=Personal Use' -keyalg RSA -keysize 3072 -validity 10000 -noprompt
    CheckExit 'Signing key generation'
}
$apk = Join-Path $workspaceRoot 'outputs\Luma.apk'
& $java -jar (Join-Path $buildTools 'lib\apksigner.jar') sign --ks $keyStore --ks-key-alias personal --ks-pass pass:focus-personal-local --key-pass pass:focus-personal-local --out $apk $aligned
CheckExit 'APK signing'
& $java -jar (Join-Path $buildTools 'lib\apksigner.jar') verify --verbose $apk
CheckExit 'APK verification'
Get-Item -LiteralPath $apk | Select-Object FullName,Length
Get-FileHash -LiteralPath $apk -Algorithm SHA256
