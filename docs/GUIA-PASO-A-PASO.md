# Guía paso a paso

Esta guía asume que **nunca has usado ADB ni Android Studio**. Tiempo aproximado: 1–2 horas, casi todo esperando descargas.

**Necesitas:**
- Un Amazon Fire TV Stick (probado en 3.ª gen, Fire OS 7).
- Una PC con Windows y **al menos 15 GB libres** (Android Studio + SDK).
- Tu celular con la app **Amazon Fire TV** (por si no tienes el control).
- Todo en la **misma red Wi‑Fi**.

---

## 1. Prepara el Fire TV

1. **Configuración → Mi Fire TV → Acerca de**. Confirma el modelo.
2. Presiona **7 veces** sobre el nombre del dispositivo hasta que diga que eres desarrollador.
3. Regresa a **Mi Fire TV → Opciones de desarrollador** y activa **Depuración ADB**.
4. En **Acerca de → Red**, anota la **dirección IP** (ej. `192.168.1.50`).

## 2. Instala adb y conéctate

En PowerShell:

```powershell
winget install Google.PlatformTools
```

Cierra y abre PowerShell. Si `adb` no se reconoce, búscalo y entra a su carpeta:

```powershell
Get-ChildItem $env:LOCALAPPDATA -Filter adb.exe -Recurse -Depth 6 -ErrorAction SilentlyContinue | Select-Object -First 1 FullName
```

Conéctate (en PowerShell se usa `.\adb` dentro de la carpeta):

```powershell
.\adb connect 192.168.1.50:5555
.\adb devices
```

En la tele aparece **"¿Permitir depuración?"** → marca *Permitir siempre* → Aceptar. Si se quitó antes de aceptarlo: `.\adb disconnect` y conecta de nuevo.

Prueba (aquí está el momento "wow"):

```powershell
.\adb shell input keyevent KEYCODE_HOME
.\adb shell input keyevent KEYCODE_DPAD_RIGHT
```

## 3. Diseña tu menú (opcional, pero es lo divertido)

🎨 **Aquí es donde Claude te ayuda a diseñar tu propio menú** con los colores y archivos gráficos que tú quieras. Usa el [prompt 3](PROMPTS.md#3-diseña-tu-menú) y, cuando te guste, pídele que lo pase a `launcher/app/src/main/assets/index.html`.

Si no, usa el menú PROCASTIA tal cual y solo cambia `USER_NAME` y `MISSION` al inicio de `index.html`.

## 4. Instala Android Studio

```powershell
winget install Google.AndroidStudio
```

Ábrelo (si no aparece en Inicio: `& "C:\Program Files\Android\Android Studio\bin\studio64.exe"`), elige **Standard**, acepta todas las licencias y deja que descargue el SDK.

## 5. Abre y compila el proyecto

1. **Open** → elige la carpeta `launcher` del repo → **Trust Project**.
2. Si aparece "Gradle build script found" o el aviso **Link Gradle project**, dale clic.
3. Si pide **Download JDK**, elige la versión **21**.
4. Espera el **Gradle sync** (la primera vez 5–15 min) hasta ver *BUILD SUCCESSFUL*.

Ahora la forma fácil, desde la raíz del repo:

```powershell
.\scripts\instalar.ps1 -FireTvIp 192.168.1.50
```

O a mano:

```powershell
cd launcher
$env:JAVA_HOME="$env:USERPROFILE\.jdks\<tu-jdk-21>"
.\gradlew.bat assembleDebug
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb connect 192.168.1.50:5555
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell appops set com.procastia.launcher GET_USAGE_STATS allow
& $adb shell am start -n com.procastia.launcher/.MainActivity
```

## 6. Tu control en el iPhone

📱 **Aquí también puedes coordinar a Claude para que diseñe tu propio control** ([prompt 5](PROMPTS.md#5-diseña-tu-control)). El que viene incluido ya funciona:

1. iPhone en el mismo Wi‑Fi.
2. Safari → `http://192.168.1.50:8765` (la IP de tu Fire TV).
3. **Compartir → Agregar a pantalla de inicio**.
4. Abre YouTube en la tele, toca **HOME** en tu app… y aparece tu menú.

Prueba desde la PC si algo no responde:

```powershell
curl.exe "http://192.168.1.50:8765/k?b=home"
```

Debe responder `Home: PROCASTIA`.

---

## Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| `adb` no se reconoce | winget no lo agregó al PATH | Entra a su carpeta y usa `.\adb` |
| El aviso de depuración se quitó | Duró poco | `adb disconnect` y `adb connect` otra vez |
| Android Studio: *Missing essential plugins* | Instalación incompleta o config vieja | Cierra Android Studio en el Administrador de tareas, renombra `%APPDATA%\Google\AndroidStudio*` y reinstala |
| winget: *database or disk is full* | **Disco lleno** | Libera 15 GB+ (instaladores viejos, ISOs, videos ya publicados) |
| Android Studio no compila nada | Proyecto no vinculado a Gradle | Abre `settings.gradle.kts` → **Link Gradle project** |
| *Lint found fatal errors* | Revisión de release estricta | Ya viene desactivada en `app/build.gradle.kts` (`lint { checkReleaseBuilds = false }`) |
| Gradle dice *up-to-date* y no cambia nada | El archivo editado no se guardó | Revisa que el cambio esté en el archivo y compila otra vez |
| Safari: *no se puede analizar la respuesta* | Otro servicio de Amazon usa el puerto 8080 | Por eso la app usa el **8765** |
| Safari: *se perdió la conexión* | Servidor viejo | `adb shell am force-stop com.procastia.launcher` y ábrela de nuevo |
| Home sigue llevando al de Amazon | Falta el permiso | `adb shell appops set com.procastia.launcher GET_USAGE_STATS allow` |

## Para volver a como estaba

```powershell
& $adb shell appops set com.procastia.launcher GET_USAGE_STATS deny
& $adb uninstall com.procastia.launcher
```
