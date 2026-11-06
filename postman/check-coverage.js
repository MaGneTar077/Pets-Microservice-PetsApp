#!/usr/bin/env node
/*
 * Compara los endpoints declarados en los controladores con los requests de la
 * colección de Postman. Sale con código 1 si falta o sobra alguno.
 *
 * Fuente de verdad: el código. Recorre src/main/java buscando @RestController, toma el
 * @RequestMapping de la clase como base y lo concatena con el path de cada
 * @GetMapping/@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping.
 *
 * Del lado de Postman recorre el árbol de items (incluidas las subcarpetas 06.1/06.2/06.3) y
 * normaliza cada url.raw: quita el prefijo {{pets_base}} o {{pets_direct_url}}, el query string, y
 * convierte {{variable}} en {variable}. Las path variables se normalizan a {id} porque el nombre
 * de la variable de Postman no tiene por qué coincidir con el del @PathVariable del controlador.
 *
 * Los requests del User service (carpeta 00, {{user_url}}) se ignoran: no son endpoints de este
 * servicio. Varios requests pueden apuntar al mismo endpoint (subir por tipo de documento, las
 * pruebas de permisos de la carpeta 08): eso no cuenta como "sobrante", solo como variantes.
 *
 * Uso:
 *   node postman/check-coverage.js
 *   node postman/check-coverage.js --table
 */

const fs = require('fs');
const path = require('path');

const SOURCE_ROOT = 'src/main/java';
const COLLECTION = 'postman/MyAnimaLogPets.postman_collection.json';
const HOST_VARS = ['pets_base', 'pets_direct_url'];
const SHOW_TABLE = process.argv.includes('--table');

// ---------- 1. Endpoints declarados en el código ----------

function javaFiles(dir) {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...javaFiles(full));
    else if (entry.name.endsWith('.java')) out.push(full);
  }
  return out;
}

/** Acepta ("/x"), (value = "/x"), (path = "/x"), ("/x", consumes = ...) y (). */
function mappingPath(annotationArgs) {
  if (!annotationArgs || !annotationArgs.trim()) return '';
  const m = annotationArgs.match(/(?:(?:value|path)\s*=\s*)?"([^"]*)"/);
  return m ? m[1] : '';
}

function joinRoute(base, sub) {
  let route = `${base.replace(/\/+$/, '')}/${sub.replace(/^\/+/, '')}`.replace(/\/{2,}/g, '/');
  if (route.length > 1) route = route.replace(/\/+$/, '');
  return route.startsWith('/') ? route : `/${route}`;
}

/**
 * Normaliza path variables. Además de "{petId}" -> "{id}", colapsa segmentos en MAYUSCULAS (p.ej.
 * "/type/VACCINE") a "{id}": son los valores fijos de enum (DocumentType) que usan los requests de
 * "06.2/06.3 · por tipo" en vez de una variable de Postman, para que sigan contando como el mismo
 * endpoint "GET /pets/{petId}/documents/type/{documentType}" del código.
 */
const normalize = (method, route) =>
  `${method} ${route.replace(/\{[^}]+\}/g, '{id}').replace(/\/[A-Z][A-Z_]*(?=\/|$)/g, '/{id}')}`;

const codeEndpoints = [];
for (const file of javaFiles(SOURCE_ROOT)) {
  const content = fs.readFileSync(file, 'utf8');
  if (!content.includes('@RestController')) continue;

  const baseMatch = content.match(/@RequestMapping\s*\(([^)]*)\)/);
  const base = baseMatch ? mappingPath(baseMatch[1]) : '';

  const re = /@(Get|Post|Put|Patch|Delete)Mapping(?:\s*\(((?:[^()]|\([^()]*\))*)\))?/g;
  let m;
  while ((m = re.exec(content)) !== null) {
    codeEndpoints.push({
      method: m[1].toUpperCase(),
      route: joinRoute(base, mappingPath(m[2])),
      file: path.basename(file),
    });
  }
}

const codeKeys = new Map();
for (const e of codeEndpoints) codeKeys.set(normalize(e.method, e.route), e);

console.log('');
console.log('=== Endpoints declarados en los controladores ===');
console.log(`Total: ${codeEndpoints.length} (${codeKeys.size} unicos tras normalizar path vars)`);

if (!fs.existsSync(COLLECTION)) {
  console.log('');
  console.log(`La coleccion todavia no existe (${COLLECTION}). Inventario del codigo:`);
  for (const e of [...codeEndpoints].sort((a, b) => a.route.localeCompare(b.route) || a.method.localeCompare(b.method))) {
    console.log(`  ${e.method.padEnd(6)} ${e.route.padEnd(72)} ${e.file}`);
  }
  process.exit(2);
}

// ---------- 2. Requests de la colección ----------

const collection = JSON.parse(fs.readFileSync(COLLECTION, 'utf8'));
const postmanEndpoints = [];
const ignored = [];

(function walk(items, folderPath) {
  for (const item of items || []) {
    if (item.item) {
      walk(item.item, folderPath ? `${folderPath} / ${item.name}` : item.name);
      continue;
    }
    if (!item.request) continue;

    const raw = typeof item.request.url === 'string' ? item.request.url : item.request.url.raw;
    const method = item.request.method.toUpperCase();

    const hostVar = HOST_VARS.find((v) => raw.includes(`{{${v}}}`));
    if (!hostVar) {
      ignored.push({ folder: folderPath, method, raw });
      continue;
    }

    let route = raw.replace(new RegExp(`^.*\\{\\{${hostVar}\\}\\}`), '').split('?')[0];
    route = route.replace(/\{\{([^}]+)\}\}/g, '{$1}');
    if (route.length > 1) route = route.replace(/\/+$/, '');
    if (!route.startsWith('/')) route = `/${route}`;

    postmanEndpoints.push({ folder: folderPath, method, route, name: item.name });
  }
})(collection.item, '');

const postmanKeys = new Map();
for (const e of postmanEndpoints) postmanKeys.set(normalize(e.method, e.route), e);

// ---------- 3. Comparación ----------

const missing = [...codeKeys.keys()].filter((k) => !postmanKeys.has(k)).sort();
const extra = [...postmanKeys.keys()].filter((k) => !codeKeys.has(k)).sort();

console.log(
  `Requests en la coleccion: ${postmanEndpoints.length} al servicio (${postmanKeys.size} unicos) + ${ignored.length} ignorados (User service)`
);
console.log('');

if (missing.length) {
  console.log('FALTANTES (en el codigo, no en la coleccion):');
  missing.forEach((k) => console.log(`  - ${k}`));
  console.log('');
}
if (extra.length) {
  console.log('SOBRANTES (en la coleccion, no en el codigo):');
  extra.forEach((k) => console.log(`  - ${k}`));
  console.log('');
}

if (SHOW_TABLE) {
  console.log('=== Tabla de cobertura ===');
  console.log(`${'Carpeta'.padEnd(46)} ${'Metodo'.padEnd(6)} Ruta`);
  for (const e of [...postmanEndpoints].sort(
    (a, b) => a.folder.localeCompare(b.folder) || a.route.localeCompare(b.route) || a.method.localeCompare(b.method)
  )) {
    console.log(`${e.folder.padEnd(46)} ${e.method.padEnd(6)} ${e.route}`);
  }
  console.log('');
}

if (!missing.length && !extra.length) {
  console.log('OK: 0 faltantes, 0 sobrantes.');
  process.exit(0);
}

console.log(`FALLO: ${missing.length} faltantes, ${extra.length} sobrantes.`);
process.exit(1);
