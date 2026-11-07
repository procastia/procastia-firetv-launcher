"""
Control remoto PROCASTIA para Fire TV.
Corre en tu PC y lo abres desde el celular en http://<IP-de-tu-PC>:8080

Uso:
    python control.py 192.168.1.50      (la IP de tu Fire TV)
Requisitos: Python 3 y adb (ya lo tienes). Sin librerías extra.
"""
import os
import sys
import socket
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

FIRE_TV = (sys.argv[1] if len(sys.argv) > 1 else os.environ.get("FIRE_TV_IP", "192.168.1.50")) + ":5555"
PORT = 8080
LAUNCHER = "com.procastia.launcher/.MainActivity"

# adb de Android Studio (si no existe, usa el que esté en el PATH)
ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

# Botón -> comando adb shell
KEYS = {
    "up": "input keyevent KEYCODE_DPAD_UP",
    "down": "input keyevent KEYCODE_DPAD_DOWN",
    "left": "input keyevent KEYCODE_DPAD_LEFT",
    "right": "input keyevent KEYCODE_DPAD_RIGHT",
    "ok": "input keyevent KEYCODE_DPAD_CENTER",
    "back": "input keyevent KEYCODE_BACK",
    "menu": "input keyevent KEYCODE_MENU",
    "play": "input keyevent KEYCODE_MEDIA_PLAY_PAUSE",
    "rew": "input keyevent KEYCODE_MEDIA_REWIND",
    "ff": "input keyevent KEYCODE_MEDIA_FAST_FORWARD",
    "volup": "input keyevent KEYCODE_VOLUME_UP",
    "voldown": "input keyevent KEYCODE_VOLUME_DOWN",
    "mute": "input keyevent KEYCODE_VOLUME_MUTE",
    "power": "input keyevent KEYCODE_SLEEP",
    # El truco: Home abre TU menú, no el de Amazon
    "home": f"am start -n {LAUNCHER}",
    "youtube": "monkey -p com.amazon.firetv.youtube 1",
    "netflix": "monkey -p com.netflix.ninja 1",
}

def adb(*args):
    return subprocess.run([ADB, "-s", FIRE_TV, *args],
                          capture_output=True, text=True, timeout=10)

def ensure_connected():
    subprocess.run([ADB, "connect", FIRE_TV], capture_output=True, text=True, timeout=10)


PAGE = r"""<!doctype html><html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<meta name="apple-mobile-web-app-capable" content="yes">
<title>PROCASTIA Remote</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@500;700&family=Silkscreen&display=swap" rel="stylesheet">
<style>
:root{--a:#E2B44B;--bg:#0B1712;--c:#12241B;--l:#2E4A3B}
*{box-sizing:border-box;-webkit-tap-highlight-color:transparent;user-select:none}
body{margin:0;background:var(--bg);color:#EDEFE9;font-family:Inter,sans-serif;min-height:100vh;display:flex;justify-content:center}
.w{width:100%;max-width:380px;padding:24px 20px 32px;display:flex;flex-direction:column;gap:22px}
.px{font-family:Silkscreen,monospace}
h1{margin:0;font-size:22px;color:var(--a);font-weight:400;letter-spacing:2px;text-align:center}
#st{font-size:13px;color:#8C9A90;text-align:center;min-height:16px}
button{font:inherit;color:inherit;background:var(--c);border:3px solid var(--l);border-radius:0;font-size:18px;font-weight:700;min-height:56px;cursor:pointer}
button:active{background:var(--l);border-color:var(--a)}
.row{display:grid;gap:12px}
.r3{grid-template-columns:repeat(3,1fr)}.r2{grid-template-columns:repeat(2,1fr)}
.pad{display:grid;grid-template-columns:repeat(3,1fr);grid-template-rows:repeat(3,78px);gap:8px}
.pad button{font-size:26px}
.ok{background:var(--a);color:var(--bg);border-color:var(--a);font-family:Silkscreen,monospace}
.home{background:var(--a);color:var(--bg);border-color:var(--a)}
</style></head><body><div class="w">
<h1 class="px">PROCASTIA REMOTE</h1>
<div class="row r3"><button data-k="power">⏻</button><button data-k="home" class="home">HOME</button><button data-k="menu">☰</button></div>
<div class="pad">
<span></span><button data-k="up">▲</button><span></span>
<button data-k="left">◀</button><button data-k="ok" class="ok">OK</button><button data-k="right">▶</button>
<span></span><button data-k="down">▼</button><span></span>
</div>
<div class="row r3"><button data-k="back">↩</button><button data-k="play">⏯</button><button data-k="mute">🔇</button></div>
<div class="row r3"><button data-k="rew">⏪</button><button data-k="voldown">VOL −</button><button data-k="volup">VOL +</button></div>
<div class="row r2"><button data-k="youtube">YouTube</button><button data-k="netflix">Netflix</button></div>
<div id="st"></div>
</div>
<script>
var st=document.getElementById('st');
document.querySelectorAll('button[data-k]').forEach(function(b){
  b.addEventListener('click',function(){
    if(navigator.vibrate)navigator.vibrate(15);
    fetch('/k?b='+b.dataset.k).then(function(r){return r.text()}).then(function(t){st.textContent=t})
    .catch(function(){st.textContent='Sin conexión con la PC'});
  });
});
</script></body></html>"""


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        url = urlparse(self.path)
        if url.path == "/k":
            key = parse_qs(url.query).get("b", [""])[0]
            cmd = KEYS.get(key)
            if not cmd:
                return self._send(400, "Botón desconocido")
            r = adb("shell", *cmd.split())
            if r.returncode != 0:
                ensure_connected()
                r = adb("shell", *cmd.split())
            msg = "OK: " + key if r.returncode == 0 else "Error: " + (r.stderr or r.stdout).strip()
            return self._send(200, msg)
        self._send(200, PAGE, "text/html; charset=utf-8")

    def _send(self, code, body, ctype="text/plain; charset=utf-8"):
        data = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


def local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        s.close()


if __name__ == "__main__":
    ensure_connected()
    print("PROCASTIA Remote listo.")
    print(f"Abre en tu celular:  http://{local_ip()}:{PORT}")
    print("Ctrl+C para detener.")
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
