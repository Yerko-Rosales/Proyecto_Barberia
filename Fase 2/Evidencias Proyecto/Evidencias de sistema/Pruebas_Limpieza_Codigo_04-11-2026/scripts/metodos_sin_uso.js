// Lista métodos de objetos/clases (forma `nombre(args) {`) cuyo nombre no aparece en ningún otro lugar del código.
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
const sinComentarios = src
  .replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' '))
  .replace(/(^|[^:'"`\\])\/\/[^\n]*/gm, '$1');
const lineas = sinComentarios.split('\n');
const RESERVADAS = new Set(['if', 'for', 'while', 'switch', 'catch', 'function', 'return', 'constructor', 'render', 'componentDidCatch', 'getDerivedStateFromError']);
for (let i = 0; i < lineas.length; i++) {
  const m = lineas[i].match(/^\s+(?:async\s+|static\s+|get\s+)?([A-Za-z_$][\w$]*)\s*\([^)]*\)\s*\{\s*$/);
  if (!m || RESERVADAS.has(m[1])) continue;
  const usos = (sinComentarios.match(new RegExp('\\b' + m[1].replace('$', '\\$') + '\\b', 'g')) || []).length;
  if (usos <= 1) console.log(`${i + 1} ${m[1]}`);
}
