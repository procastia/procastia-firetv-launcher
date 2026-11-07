# Compila e instala el launcher PROCASTIA en tu Fire TV (Windows / PowerShell)
#
# Uso (desde la carpeta raíz del repo):
#   .\scripts\instalar.ps1 -FireTvIp 192.168.1.50
#
# Requisitos: Android Studio instalado y abierto una vez (SDK descargado),
# y "Depuración ADB" activada en el Fire TV.

param(
    [Parameter(Mandatory = $true)][string]$FireTvIp
)

$ErrorActionPreference = "Stop"

# 1. Java: usa el que trae Android Studio si no tienes JAVA_HOME
if (-not $env:JAVA_HOME) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    $jdk21 = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -Filter "*21*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdk21) { $env:JAVA_HOME = $jdk21.FullName }
    elseif (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
}
Write-Host "Java: $env:JAVA_HOME"

# 2. adb del SDK de Android
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { $adb = "adb" }

# 3. Compilar
Push-Location "$PSScriptRoot\..\launcher"
try {
    .\gradlew.bat assembleDebug
} finally {
    Pop-Location
}
$apk = "$PSScriptRoot\..\launcher\app\build\outputs\apk\debug\app-debug.apk"

# 4. Instalar en el Fire TV
& $adb connect "$($FireTvIp):5555"
Write-Host "Si en la tele aparece 'Permitir depuracion', aceptalo y vuelve a correr el script."
& $adb install -r $apk

# 5. Permiso para detectar el boton Home de Amazon y abrir el menu
& $adb shell appops set com.procastia.launcher GET_USAGE_STATS allow

# 6. Abrir el menu (esto tambien enciende el control remoto web)
& $adb shell am force-stop com.procastia.launcher
& $adb shell am start -n com.procastia.launcher/.MainActivity

Write-Host ""
Write-Host "Listo. Abre en tu celular (mismo Wi-Fi):  http://$($FireTvIp):8765"
Write-Host "En iPhone: Safari > Compartir > Agregar a pantalla de inicio."
