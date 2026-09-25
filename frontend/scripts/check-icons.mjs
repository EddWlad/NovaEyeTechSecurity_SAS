// Verifica que todo icono usado en las plantillas este en la lista `icon_names` de src/index.html.
//
// La fuente de iconos se pide con solo los iconos que la app usa (6 KB en vez de 316 KB). Un icono que
// falte en esa lista no falla: se pinta como texto ("visibility") y se ve roto. Este chequeo lo
// detecta en el build antes de que llegue a produccion.
//
// Uso: npm run check:icons
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const APP_DIR = 'src/app';
const INDEX = 'src/index.html';

const index = readFileSync(INDEX, 'utf8');
const listed = index.match(/icon_names=([a-z0-9_,]+)/)?.[1]?.split(',');
if (!listed) {
  console.error(`No se encontro icon_names en ${INDEX}`);
  process.exit(1);
}

const files = [];
(function walk(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) walk(path);
    else if (/\.(html|ts)$/.test(name)) files.push(path);
  }
})(APP_DIR);

// Cada icono usado, con el archivo donde aparece.
const used = new Map();
const add = (icon, file) => used.set(icon, used.get(icon) ?? file);

for (const file of files) {
  const source = readFileSync(file, 'utf8');

  // <span class="material-symbols-outlined">nombre</span>
  for (const m of source.matchAll(/material-symbols-outlined[^>]*>\s*([a-z0-9_]+)\s*</g)) add(m[1], file);

  // <span class="material-symbols-outlined">{{ cond ? 'a' : 'b' }}</span>: cada literal es un icono.
  for (const m of source.matchAll(/material-symbols-outlined[^>]*>\s*\{\{([^}]*)\}\}/g)) {
    for (const literal of m[1].matchAll(/'([a-z0-9_]+)'/g)) add(literal[1], file);
  }

  // Configuracion: icon: 'nombre'
  for (const m of source.matchAll(/\bicon:\s*'([a-z0-9_]+)'/g)) add(m[1], file);
}

const missing = [...used].filter(([icon]) => !listed.includes(icon));

if (missing.length) {
  console.error('Iconos usados que faltan en icon_names de src/index.html:');
  for (const [icon, file] of missing) console.error(`  - ${icon}  (${file})`);
  console.error('Agregalos a la lista, en orden alfabetico.');
  process.exit(1);
}

const sorted = [...listed].sort().join(',') === listed.join(',');
if (!sorted) {
  console.error('icon_names debe estar en orden alfabetico.');
  process.exit(1);
}

console.log(`OK: ${used.size} iconos usados, todos en icon_names (${listed.length} listados).`);
