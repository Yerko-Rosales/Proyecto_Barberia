"""Ayudante para probar la app en el emulador: textos visibles, tocar por texto, escribir y capturar."""
import subprocess, sys, re, time, os

ADB = r'C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe'
OUT = os.path.join(os.path.dirname(__file__), 'app')
os.makedirs(OUT, exist_ok=True)


def adb(*args, binary=False):
    r = subprocess.run([ADB, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode('utf-8', 'replace')


def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/ui.xml')
    xml = adb('shell', 'cat', '/sdcard/ui.xml')
    res = []
    for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t, d, x1, y1, x2, y2 = m.groups()
        res.append(((t or d), (int(x1) + int(x2)) // 2, (int(y1) + int(y2)) // 2))
    return res


def textos():
    return [t for t, _, _ in nodes() if t]


def tocar(texto, exacto=False, intentos=6):
    for _ in range(intentos):
        for t, x, y in nodes():
            if t and ((t == texto) if exacto else texto.lower() in t.lower()):
                adb('shell', 'input', 'tap', str(x), str(y))
                time.sleep(1.5)
                return True
        time.sleep(2)
    raise SystemExit(f'No encontré «{texto}». Visibles: {textos()[:40]}')


def escribir(texto):
    adb('shell', 'input', 'text', texto.replace(' ', '%s'))
    time.sleep(0.8)


def captura(nombre):
    open(os.path.join(OUT, nombre + '.png'), 'wb').write(adb('exec-out', 'screencap', '-p', binary=True))


if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'textos':
        print(textos())
    elif cmd == 'tocar':
        tocar(sys.argv[2], exacto=len(sys.argv) > 3)
    elif cmd == 'escribir':
        escribir(sys.argv[2])
    elif cmd == 'captura':
        captura(sys.argv[2])
    elif cmd == 'atras':
        adb('shell', 'input', 'keyevent', '4')
