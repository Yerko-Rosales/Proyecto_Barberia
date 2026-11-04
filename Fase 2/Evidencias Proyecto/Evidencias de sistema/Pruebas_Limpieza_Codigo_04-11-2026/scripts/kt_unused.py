"""Busca en el código Kotlin: imports sin uso y declaraciones (fun/val/var/class/object) nunca referenciadas.
Uso: kt_unused.py <src/main> [<otras carpetas que cuentan como uso, p. ej. src/test>]"""
import os, re, sys

def leer(carpeta):
    out = {}
    for d, _, fs in os.walk(carpeta):
        for f in fs:
            if f.endswith('.kt'):
                p = os.path.join(d, f)
                out[p] = open(p, encoding='utf-8').read()
    return out

def strip(s):
    # quita comentarios conservando los saltos de línea (para que los números de línea sigan siendo válidos)
    s = re.sub(r'/\*.*?\*/', lambda m: '\n' * m.group(0).count('\n'), s, flags=re.S)
    return re.sub(r'//[^\n]*', '', s)

root = sys.argv[1]
files = leer(root)
extra = {}
for c in sys.argv[2:]:
    extra.update(leer(c))
code = {p: strip(s) for p, s in files.items()}
alltext = '\n'.join(list(code.values()) + [strip(s) for s in extra.values()])
OPS = {'getValue', 'setValue', 'provideDelegate', 'invoke', 'plus', 'minus', 'times', 'div', 'contains',
       'compareTo', 'iterator', 'rangeTo', 'get', 'set', 'next', 'hasNext'}

print('== imports sin uso')
for p, s in code.items():
    body = re.sub(r'^import [^\n]*', '', s, flags=re.M)
    for m in re.finditer(r'^import ([\w.]+)(?: as (\w+))?', s, flags=re.M):
        name = m.group(2) or m.group(1).split('.')[-1]
        if name in OPS:
            continue
        if not re.search(r'\b' + re.escape(name) + r'\b', body):
            print(f'{os.path.relpath(p, root)}|{m.group(0)}')

print('== declaraciones sin uso')
decl = re.compile(r'^[ \t]*((?:private|internal|public|protected|override|suspend|inline|data|enum|sealed|abstract|open|const|lateinit|operator|infix|tailrec)\s+)*(fun|val|var|class|object|interface)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)', re.M)
for p, s in code.items():
    for m in decl.finditer(s):
        if 'override' in (m.group(0) or '') or m.group(3) in OPS or m.group(3) == 'MainActivity':
            continue
        name = m.group(3)
        if len(re.findall(r'\b' + re.escape(name) + r'\b', alltext)) <= 1:
            print(f'{os.path.relpath(p, root)}:{name}  (línea {s[:m.start()].count(chr(10)) + 1}, {m.group(2)})')
