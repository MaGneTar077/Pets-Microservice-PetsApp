#!/usr/bin/env node
/*
 * Genera postman/MyAnimaLogPets.postman_collection.json (Postman Collection v2.1).
 *
 * La colección se genera en vez de editarse a mano para que la estructura repetitiva
 * (auth por carpeta, scripts de test, ejemplos) no se desincronice entre requests.
 * Después de tocar este archivo:
 *
 *   node postman/build-collection.js && node postman/check-coverage.js --table
 *
 * Fuente de verdad de las rutas: los controladores (src/main/java/.../infrastructure/controllers).
 * check-coverage.js lo verifica. Las reglas de permisos y los codes de error vienen de
 * docs/integracion/PETS_CAMBIOS_PARA_FRONTEND.md y CLAUDE.md, pero si contradicen al código, manda
 * el código.
 */

const fs = require('fs');

// ---------- helpers ----------

const bearer = (variable) => ({
  type: 'bearer',
  bearer: [{ key: 'token', value: `{{${variable}}}`, type: 'string' }],
});

const noAuth = { type: 'noauth' };

/** Convierte "{{pets_base}}/pets/{{petId}}?x=1" en el objeto url de v2.1. */
function parseUrl(raw, query) {
  const [base, qs] = raw.split('?');
  const segments = base.replace(/^\{\{[a-z_]+\}\}\/?/, '').split('/').filter(Boolean);
  const hostVar = (base.match(/^\{\{([a-z_]+)\}\}/) || [])[1];
  const url = { raw, host: [`{{${hostVar}}}`], path: segments };
  if (query && query.length) {
    url.query = query.map((q) => ({
      key: q.key,
      value: q.value,
      description: q.description,
      disabled: q.disabled === true,
    }));
  } else if (qs) {
    url.query = qs.split('&').map((pair) => {
      const [key, value] = pair.split('=');
      return { key, value };
    });
  }
  return url;
}

function jsonBody(obj) {
  return {
    mode: 'raw',
    raw: JSON.stringify(obj, null, 2),
    options: { raw: { language: 'json' } },
  };
}

function formdataBody(fields) {
  return {
    mode: 'formdata',
    formdata: fields.map((f) => ({
      key: f.key,
      type: f.type || 'text',
      value: f.value,
      src: f.src,
      description: f.description,
      disabled: f.disabled === true,
    })),
  };
}

function testScript(lines) {
  return [
    {
      listen: 'test',
      script: { type: 'text/javascript', exec: lines },
    },
  ];
}

/** Test estándar: verifica el status y, opcionalmente, guarda un id de la respuesta. */
function expectStatus(status, saves = []) {
  const lines = [
    `pm.test("status ${status}", function () {`,
    `    pm.response.to.have.status(${status});`,
    '});',
  ];
  if (saves.length) {
    lines.push('');
    lines.push('if (pm.response.code === ' + status + ') {');
    lines.push('    const body = pm.response.json();');
    for (const s of saves) {
      lines.push(`    if (${s.path}) {`);
      lines.push(`        pm.environment.set("${s.variable}", ${s.path});`);
      lines.push(`        console.log("${s.variable} =", ${s.path});`);
      lines.push('    }');
    }
    lines.push('}');
  }
  return testScript(lines);
}

/** Test de error: verifica status y, si viene, el code del body. */
function expectError(status, code) {
  const lines = [
    `pm.test("status ${status}${code ? ' ' + code : ''}", function () {`,
    `    pm.response.to.have.status(${status});`,
  ];
  if (code) {
    lines.push('    const body = pm.response.json();');
    lines.push(`    pm.expect(body.code).to.eql("${code}");`);
  }
  lines.push('});');
  return testScript(lines);
}

function example({ name, status, code, body, req }) {
  return {
    name,
    originalRequest: req,
    status: code || 'OK',
    code: status,
    _postman_previewlanguage: 'json',
    header: [{ key: 'Content-Type', value: 'application/json' }],
    cookie: [],
    body: JSON.stringify(body, null, 2),
  };
}

function request({ name, method, raw, description, body, query, tests, examples, headers, auth }) {
  const req = {
    method,
    header: headers || (body && body.mode === 'raw' ? [{ key: 'Content-Type', value: 'application/json' }] : []),
    url: parseUrl(raw, query),
    description,
  };
  if (body) req.body = body;
  if (auth) req.auth = auth;

  const item = { name, request: req };
  if (tests) item.event = tests;
  if (examples) {
    item.response = examples.map((e) =>
      example({ ...e, req: { ...req, url: parseUrl(raw, query) } })
    );
  }
  return item;
}

function folder({ name, description, auth, items }) {
  const f = { name, description, item: items };
  if (auth) f.auth = auth;
  return f;
}

const ERR_401 = '`401` — falta el `Authorization: Bearer`, el token está vencido o la firma es inválida.';

/** code → status HTTP, para no repetirlo en cada endpoint (ver GlobalExceptionHandler). */
const CODES = {
  IDENTITY_MISMATCH: 403,
  PET_ACCESS_DENIED: 403,
  NOT_PET_OWNER: 403,
  DOCUMENT_NOT_IN_PET: 403,
  CANNOT_SHARE_WITH_OWNER: 400,
  ACCESS_ALREADY_GRANTED: 409,
  INVITATION_EMAIL_MISMATCH: 403,
  EMAIL_NOT_VERIFIED: 403,
};

const ERR_IDENTITY = [
  '**Errores de identidad comunes (PETS-02)**',
  '- `401` — falta el token o está vencido.',
  '- `403 IDENTITY_MISMATCH` — enviaste un id/email legado que no es el tuyo (ni, cuando aplica, el',
  '  del dueño real de la mascota).',
].join('\n');

const DOC_TYPES = [
  { type: 'VACCINE', label: 'vacuna', fileName: 'vacuna-antirrabica.pdf', mime: 'application/pdf' },
  { type: 'DEWORMING', label: 'desparasitación', fileName: 'desparasitacion.pdf', mime: 'application/pdf' },
  { type: 'LAB_RESULT', label: 'resultado de laboratorio', fileName: 'examen-sangre.pdf', mime: 'application/pdf' },
  { type: 'MEDICAL_RECORD', label: 'historia clínica', fileName: 'historia-clinica.pdf', mime: 'application/pdf' },
  { type: 'PRESCRIPTION', label: 'prescripción', fileName: 'receta-medica.pdf', mime: 'application/pdf' },
  { type: 'SURGERY_REPORT', label: 'informe de cirugía', fileName: 'informe-cirugia.pdf', mime: 'application/pdf' },
  { type: 'ALLERGY_REPORT', label: 'reporte de alergia', fileName: 'reporte-alergia.pdf', mime: 'application/pdf' },
  { type: 'OTHER', label: 'otro documento', fileName: 'otro-documento.pdf', mime: 'application/pdf' },
];

const NOTE_FILE_FIELD = [
  '⚠️ Al importar esta colección, Postman no conserva la ruta del archivo de ejemplo (no existe en',
  'tu computadora): abrí el campo `file` del body y usá **"Select files"** para elegir uno real antes',
  'de enviar.',
].join('\n');

// ---------- 00 · Autenticación (User service) ----------

const auth00 = folder({
  name: '00 · Autenticación (User service)',
  auth: noAuth,
  description: [
    'Lo mínimo para conseguir tokens. **Estos endpoints son del User service**, no de Pets, y usan',
    '`{{user_url}}`. check-coverage.js los ignora: no son endpoints de este servicio.',
    '',
    '⚠️ Las rutas de esta carpeta **no están verificadas contra el código del User service** (este',
    'repo es solo Pets). Si no coinciden con tu User service, ajustalas.',
    '',
    'Orden: **Login** (guarda `access_token`) → **Login (segunda cuenta)** (guarda',
    '`second_user_token`) → **Mi usuario (segunda cuenta)** (guarda `second_user_id`, necesario para',
    'compartir y para la carpeta 08).',
  ].join('\n'),
  items: [
    request({
      name: 'POST Login',
      method: 'POST',
      raw: '{{user_url}}/auth/local',
      description: [
        'Login de la **primera cuenta** (la que será dueña de la mascota de prueba). Guarda',
        '`access_token` y `refresh_token`.',
      ].join('\n'),
      body: jsonBody({ email: '{{owner_email}}', password: '{{owner_password}}' }),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const token = body.accessToken || body.access_token || body.token;',
        '    if (token) {',
        '        pm.environment.set("access_token", token);',
        '        console.log("access_token guardado");',
        '    } else {',
        '        console.warn("No encontre el token en la respuesta; ajusta el script al contrato real del User service");',
        '    }',
        '    const refresh = body.refreshToken || body.refresh_token;',
        '    if (refresh) pm.environment.set("refresh_token", refresh);',
        '}',
      ]),
      examples: [
        {
          name: '200 · Login correcto',
          status: 200,
          body: {
            accessToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...',
            refreshToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...',
            expiresIn: 900,
          },
        },
      ],
    }),
    request({
      name: 'POST Login (segunda cuenta)',
      method: 'POST',
      raw: '{{user_url}}/auth/local',
      description: [
        'Login de la **segunda cuenta**: la que probará ser EDITOR/VIEWER (carpeta 03) y la que hace',
        'de "usuario ajeno" en la carpeta 08. Guarda `second_user_token`.',
        '',
        'Tiene que ser una cuenta distinta de `owner_email`, con correo **verificado** (lo exige',
        'aceptar/rechazar invitaciones).',
      ].join('\n'),
      body: jsonBody({ email: '{{second_user_email}}', password: '{{second_user_password}}' }),
      tests: testScript([
        'pm.test("status 200", function () {',
        '    pm.response.to.have.status(200);',
        '});',
        '',
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const token = body.accessToken || body.access_token || body.token;',
        '    if (token) {',
        '        pm.environment.set("second_user_token", token);',
        '        console.log("second_user_token guardado");',
        '    }',
        '}',
      ]),
      examples: [
        {
          name: '200 · Login correcto',
          status: 200,
          body: { accessToken: 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...', expiresIn: 900 },
        },
      ],
    }),
    request({
      name: 'POST Refresh del token',
      method: 'POST',
      raw: '{{user_url}}/auth/refresh',
      description: 'Renueva `access_token` con `refresh_token`. Ruta **no verificada** contra el código del User service.',
      body: jsonBody({ refreshToken: '{{refresh_token}}' }),
      tests: testScript([
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const token = body.accessToken || body.access_token || body.token;',
        '    if (token) pm.environment.set("access_token", token);',
        '}',
      ]),
    }),
    request({
      name: 'GET Mi usuario',
      method: 'GET',
      raw: '{{user_url}}/auth/me',
      description: [
        'Confirma con qué cuenta estás autenticado y si el correo está verificado (`email_verified`',
        'es obligatorio para aceptar/rechazar invitaciones — ver carpeta 05). Ruta **no verificada**',
        'contra el código del User service.',
      ].join('\n'),
      headers: [{ key: 'Authorization', value: 'Bearer {{access_token}}' }],
    }),
    request({
      name: 'GET Mi usuario (segunda cuenta)',
      method: 'GET',
      raw: '{{user_url}}/auth/me',
      description: [
        'Igual que el anterior pero con `second_user_token`: sirve para conseguir el `userId` de la',
        'segunda cuenta y guardarlo en `second_user_id` — lo usan **03 · Compartir y accesos** (para',
        'saber a quién dar acceso) y las pruebas de la carpeta 08.',
        '',
        'Ruta **no verificada** contra el código del User service; ajustá el script si el campo del id',
        'no se llama `id`/`userId`/`sub`.',
      ].join('\n'),
      headers: [{ key: 'Authorization', value: 'Bearer {{second_user_token}}' }],
      tests: testScript([
        'if (pm.response.code === 200) {',
        '    const body = pm.response.json();',
        '    const id = body.id || body.userId || body.sub;',
        '    if (id) {',
        '        pm.environment.set("second_user_id", id);',
        '        console.log("second_user_id =", id);',
        '    } else {',
        '        console.warn("No encontre el id en /auth/me; ajusta el script al contrato real del User service");',
        '    }',
        '}',
      ]),
      examples: [
        {
          name: '200 · Usuario autenticado',
          status: 200,
          body: { id: '9e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', email: 'segunda.cuenta@example.com', emailVerified: true },
        },
      ],
    }),
  ],
});

// ---------- 03 · Compartir y accesos ----------

const f03 = folder({
  name: '03 · Compartir y accesos',
  auth: bearer('access_token'),
  description: [
    'Token: **tuyo** (dueño) salvo donde se indica. Requiere `second_user_id` (carpeta 00 ·',
    '"GET Mi usuario (segunda cuenta)").',
    '',
    'Orden pensado para dejar, al final de la carpeta, a la segunda cuenta **sin** acceso (así la',
    'carpeta 08 la puede usar como "usuario ajeno"): dar acceso → cambiar rol → revocar → consultar.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Dar acceso (rol EDITOR)',
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/access',
      description: [
        'Permiso: **solo el dueño**. `ownerId` en el body es legado y opcional (misma regla de',
        'identidad).',
        '',
        `- \`400 CANNOT_SHARE_WITH_OWNER\` — \`targetUserId\` es el propio dueño.`,
        `- \`409 ACCESS_ALREADY_GRANTED\` — ese usuario ya tiene acceso a la mascota.`,
      ].join('\n'),
      body: jsonBody({ targetUserId: '{{second_user_id}}', accessRole: 'EDITOR' }),
      tests: expectStatus(201, [{ variable: 'accessId', path: 'body.id' }]),
      examples: [
        {
          name: '201 · Acceso otorgado',
          status: 201,
          code: 'Created',
          body: { id: '33cc44dd-55ee-4ff6-8007-112233445566', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', userId: '{{second_user_id}}', accessRole: 'EDITOR', createdAt: '2026-11-05T10:10:00' },
        },
        {
          name: '400 · No se puede compartir con el dueño',
          status: 400,
          code: 'Bad Request',
          body: { error: 'Cannot grant access to the pet owner', code: 'CANNOT_SHARE_WITH_OWNER' },
        },
      ],
    }),
    request({
      name: 'POST Dar acceso (rol VIEWER)',
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/access',
      description: [
        'Mismo endpoint que arriba, con `accessRole: VIEWER`, para que quede documentado el otro rol.',
        '',
        '⚠️ Usa **otro** `targetUserId` (completá `{{targetUserId}}` a mano con un tercer usuario) —',
        'si usás el mismo `second_user_id` del request anterior, responde `409',
        'ACCESS_ALREADY_GRANTED` porque ya tiene acceso.',
      ].join('\n'),
      body: jsonBody({ targetUserId: '{{targetUserId}}', accessRole: 'VIEWER' }),
      tests: expectStatus(201),
      examples: [
        {
          name: '201 · Acceso otorgado',
          status: 201,
          code: 'Created',
          body: { id: '44dd55ee-66ff-5007-9118-223344556677', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', userId: '{{targetUserId}}', accessRole: 'VIEWER', createdAt: '2026-11-05T10:11:00' },
        },
      ],
    }),
    request({
      name: 'PATCH Cambiar rol',
      method: 'PATCH',
      raw: '{{pets_base}}/pets/{{petId}}/access/{{second_user_id}}',
      description: [
        'Permiso: **solo el dueño**. Cambia el rol de `second_user_id` (otorgado arriba como EDITOR)',
        'a VIEWER, para probar luego que pierde la capacidad de editar documentos que subió como',
        'EDITOR (ver carpeta 06).',
        '',
        `- \`400 CANNOT_SHARE_WITH_OWNER\` — \`targetUserId\` es el dueño.`,
        '- `404 AccessNotFoundException` — ese usuario no tiene acceso todavía.',
      ].join('\n'),
      body: jsonBody({ newRole: 'VIEWER' }),
      tests: expectStatus(200),
      examples: [
        { name: '200 · Rol actualizado', status: 200, body: { id: '33cc44dd-55ee-4ff6-8007-112233445566', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', userId: '{{second_user_id}}', newRole: 'VIEWER', updatedAt: '2026-11-05T10:12:00' } },
      ],
    }),
    request({
      name: 'DELETE Revocar acceso',
      method: 'DELETE',
      raw: '{{pets_base}}/pets/{{petId}}/access/{{second_user_id}}',
      description: [
        'Permiso: **solo el dueño**. `ownerId` (query) opcional, misma regla de identidad.',
        '',
        'Deja a `second_user_id` sin acceso a la mascota — necesario para que las pruebas de permisos',
        'de la carpeta 08 sean representativas de un usuario realmente ajeno.',
      ].join('\n'),
      query: [{ key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(200),
      examples: [{ name: '200 · Acceso revocado', status: 200, body: { message: 'Access revoked successfully' } }],
    }),
    request({
      name: 'GET Ver con quién se comparte',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/shared',
      description: [
        'Permiso: **solo el dueño**. Lista los `PetUserAccess` de la mascota. `ownerId` (query)',
        'opcional, misma regla de identidad.',
        '',
        '⚠️ Si corriste `DELETE Revocar acceso` arriba, esta lista aparecerá vacía.',
      ].join('\n'),
      query: [{ key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(200),
      examples: [{ name: '200 · Sin accesos activos', status: 200, body: [] }],
    }),
    request({
      name: 'GET Mis mascotas compartidas',
      method: 'GET',
      raw: '{{pets_base}}/pets/shared/me',
      description: [
        'Lista las mascotas compartidas **conmigo** (donde tengo un `PetUserAccess`, no soy el',
        'dueño). Se llama con **el token del usuario que recibió el acceso**: cambiá temporalmente el',
        'auth de este request a `Bearer {{second_user_token}}` para verlo desde su perspectiva.',
        '',
        '⚠️ Si ya revocaste el acceso de `second_user_id` arriba, esta lista aparecerá vacía para esa',
        'cuenta — es el comportamiento esperado, no un error.',
        '',
        'Oficial; sustituye al alias deprecado `GET /pets/shared?userId=` (carpeta 99).',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [{ name: '200 · Sin mascotas compartidas', status: 200, body: [] }],
    }),
  ],
});

// ---------- 01 · Mis mascotas ----------

const f01 = folder({
  name: '01 · Mis mascotas',
  auth: bearer('access_token'),
  description: [
    'Token: **tuyo** (`access_token`, dueño). CRUD básico de la mascota de prueba que usa el resto',
    'de la colección.',
    '',
    '⚠️ **Dejá el `DELETE Borrar mascota` para el final** (después de probar 02–07): borra la',
    'mascota que usan las demás carpetas. Si necesitás repetir todo, volvé a correr `POST Crear',
    'mascota`.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Crear mascota',
      method: 'POST',
      raw: '{{pets_base}}/pets',
      description: [
        'Dueño = `sub` del token. `ownerId` es **obsoleto** (PETS-02): no hace falta mandarlo; si se',
        'manda, debe coincidir con el usuario autenticado o responde `403 IDENTITY_MISMATCH`.',
        '',
        'Obligatorios: `name`, `species`, `sex` (`MALE`/`FEMALE`). Opcionales: `breed`, `birthDate`,',
        '`height`, `weight`.',
        '',
        'Guarda `petId`.',
      ].join('\n'),
      body: jsonBody({
        name: 'Toby',
        species: 'Perro',
        breed: 'Labrador Retriever',
        sex: 'MALE',
        birthDate: '2022-03-15',
        height: 55.5,
        weight: 28.3,
      }),
      tests: expectStatus(201, [{ variable: 'petId', path: 'body.id' }]),
      examples: [
        {
          name: '201 · Mascota creada',
          status: 201,
          code: 'Created',
          body: {
            id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            ownerId: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9',
            name: 'Toby',
            species: 'Perro',
            breed: 'Labrador Retriever',
            sex: 'MALE',
            birthDate: '2022-03-15',
            height: 55.5,
            weight: 28.3,
            photoUrl: null,
            createdAt: '2026-11-05T10:00:00',
          },
        },
      ],
    }),
    request({
      name: 'GET Mis mascotas',
      method: 'GET',
      raw: '{{pets_base}}/pets/me',
      description: 'Lista las mascotas de las que sos **dueño** (`ownerId == sub`). Oficial; sustituye al alias deprecado `GET /pets/user/{userId}` (carpeta 99).',
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Una mascota',
          status: 200,
          body: [
            { id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', ownerId: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', name: 'Toby', species: 'Perro', breed: 'Labrador Retriever', sex: 'MALE', birthDate: '2022-03-15', height: 55.5, weight: 28.3, photoUrl: null, createdAt: '2026-11-05T10:00:00' },
          ],
        },
      ],
    }),
    request({
      name: 'GET Ver una mascota',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}',
      description: [
        'Permiso: dueño, EDITOR o VIEWER (`requireRead`).',
        '',
        `- \`403 PET_ACCESS_DENIED\` (${CODES.PET_ACCESS_DENIED}) — existe la mascota pero no tenés acceso.`,
        '- `404` — la mascota no existe.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Mascota',
          status: 200,
          body: { id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', ownerId: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', name: 'Toby', species: 'Perro', breed: 'Labrador Retriever', sex: 'MALE', birthDate: '2022-03-15', height: 55.5, weight: 28.3, photoUrl: null, createdAt: '2026-11-05T10:00:00' },
        },
      ],
    }),
    request({
      name: 'PATCH Editar mascota',
      method: 'PATCH',
      raw: '{{pets_base}}/pets/{{petId}}',
      description: [
        'Permiso: dueño o EDITOR (`requireWrite`). Actualización parcial: solo los campos no nulos se',
        'aplican.',
        '',
        '`ownerId` (query, legado, opcional) ya **no hace falta mandarlo**. Si se manda, se acepta si',
        'coincide con tu `sub` **o** con el dueño real de la mascota (para que un EDITOR pueda seguir',
        'usando el `ownerId` real que mandaba la app vieja); cualquier otro valor → `403',
        'IDENTITY_MISMATCH`.',
        '',
        `- \`403 PET_ACCESS_DENIED\` — no tenés rol de escritura sobre esta mascota.`,
        '- `400` — `name` vacío o `height`/`weight` ≤ 0.',
      ].join('\n'),
      query: [{ key: 'ownerId', value: '', description: 'Opcional, legado — ver descripción', disabled: true }],
      body: jsonBody({ name: 'Toby', breed: 'Labrador Retriever Dorado', height: 56, weight: 29 }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Mascota editada',
          status: 200,
          body: { id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', ownerId: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', name: 'Toby', species: 'Perro', breed: 'Labrador Retriever Dorado', sex: 'MALE', birthDate: '2022-03-15', height: 56, weight: 29, photoUrl: null, createdAt: '2026-11-05T10:00:00', updatedAt: '2026-11-05T10:05:00' },
        },
      ],
    }),
    request({
      name: 'GET Edad',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/age',
      description: [
        'Permiso: dueño, EDITOR o VIEWER (`requireRead`). `422` si la mascota no tiene `birthDate`',
        'registrada.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [{ name: '200 · Edad calculada', status: 200, body: { years: 4, months: 7, days: 21 } }],
    }),
    request({
      name: 'DELETE Borrar mascota',
      method: 'DELETE',
      raw: '{{pets_base}}/pets/{{petId}}',
      description: [
        'Permiso: **solo el dueño** (`requireOwner`). `ownerId` (query, legado, opcional) sigue la',
        'misma regla que en editar.',
        '',
        '⚠️ Corré este request **al final**, después de probar el resto de la colección: borra la',
        'mascota (y en cascada sus documentos/accesos) que usan las demás carpetas.',
        '',
        `- \`403 NOT_PET_OWNER\` (${CODES.NOT_PET_OWNER}) — no sos el dueño.`,
      ].join('\n'),
      query: [{ key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(204),
      examples: [{ name: '204 · Mascota eliminada', status: 204, code: 'No Content', body: {} }],
    }),
  ],
});

// ---------- 07 · Internos (servicio a servicio) ----------

const internalHeaders = [{ key: 'X-Internal-Api-Key', value: '{{internal_api_key}}' }];

const f07 = folder({
  name: '07 · Internos (servicio a servicio)',
  auth: noAuth,
  description: [
    'Autenticados con **`X-Internal-Api-Key`**, nunca con JWT — `/internal/**` rechaza tokens de',
    'usuario. **Nunca expuestos por el gateway**; los usan Medical y Veterinary para resolver dueños',
    'y leer datos básicos de mascotas en lote.',
    '',
    'Usan `{{pets_direct_url}}` (la base del servicio **sin** pasar por el gateway), a diferencia del',
    'resto de la colección que usa `{{pets_base}}`.',
  ].join('\n'),
  items: [
    request({
      name: 'GET ¿Es el dueño? (interno)',
      method: 'GET',
      raw: '{{pets_direct_url}}/internal/pets/{{petId}}/owner/{{second_user_id}}',
      description: [
        'Nunca devuelve 404: una mascota inexistente responde `200 {"isOwner": false}` (Medical trata',
        'cualquier respuesta distinta de 2xx como el servicio caído).',
      ].join('\n'),
      headers: internalHeaders,
      tests: expectStatus(200),
      examples: [{ name: '200 · No es el dueño', status: 200, body: { isOwner: false } }],
    }),
    request({
      name: 'POST Mascotas en lote (interno)',
      method: 'POST',
      raw: '{{pets_direct_url}}/internal/pets/batch',
      description: [
        'Body `{ "ids": [...] }`, máximo 100 (más → `400`, vacío → `200 []`). Omite en silencio los',
        'ids que no existen.',
        '',
        '`claimStatus` siempre `"NONE"` y `createdByVeterinaryId`/`ownerContact` siempre `null`',
        'hasta que se implemente PETS-05.',
      ].join('\n'),
      headers: internalHeaders,
      body: jsonBody({ ids: ['{{petId}}'] }),
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Una mascota',
          status: 200,
          body: [{ id: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', ownerId: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', name: 'Toby', species: 'Perro', breed: 'Labrador Retriever', sex: 'MALE', birthDate: '2022-03-15', photoUrl: null, createdByVeterinaryId: null, claimStatus: 'NONE', ownerContact: null }],
        },
      ],
    }),
  ],
});

// ---------- 08 · Pruebas de permisos (evidencia de seguridad) ----------

const f08 = folder({
  name: '08 · Pruebas de permisos (evidencia de seguridad)',
  auth: bearer('second_user_token'),
  description: [
    'Token: **de la segunda cuenta**, actuando como usuario **sin ningún acceso** a la mascota de',
    'la primera cuenta (requiere haber revocado su acceso en `03`). Todos los requests de esta',
    'carpeta **deben fallar** — son evidencia de que `PetAccessPolicy`/`CurrentUserIdentity`',
    'bloquean correctamente a un usuario ajeno.',
    '',
    'Las dos últimas (`/internal` sin key / con key falsa) van sin `Authorization` — el header',
    'inútil no cambia el resultado porque `/internal/**` no acepta JWT.',
  ].join('\n'),
  items: [
    request({
      name: 'GET Ver mascota ajena → 403',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}',
      description: 'La mascota existe pero esta cuenta no tiene rol — `requireRead` falla.',
      tests: expectError(403, 'PET_ACCESS_DENIED'),
      examples: [{ name: '403 · Sin acceso', status: 403, code: 'Forbidden', body: { error: 'You do not have access to this pet', code: 'PET_ACCESS_DENIED' } }],
    }),
    request({
      name: 'GET Ver documentos ajenos → 403',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/documents',
      description: '`requireRead` falla igual que al ver la mascota.',
      tests: expectError(403, 'PET_ACCESS_DENIED'),
      examples: [{ name: '403 · Sin acceso', status: 403, code: 'Forbidden', body: { error: 'You do not have access to this pet', code: 'PET_ACCESS_DENIED' } }],
    }),
    request({
      name: 'PATCH Editar mascota ajena (con tu propio ownerId) → 403',
      method: 'PATCH',
      raw: '{{pets_base}}/pets/{{petId}}',
      description: [
        'Mandás tu propio id en `ownerId` (coincide con el token, así que pasa la validación de',
        'identidad) pero no tenés rol de escritura sobre esta mascota → `requireWrite` falla con',
        '`PET_ACCESS_DENIED`, no `IDENTITY_MISMATCH` (ese código es para cuando el id legado no es ni',
        'el tuyo ni el del dueño real).',
      ].join('\n'),
      query: [{ key: 'ownerId', value: '{{second_user_id}}' }],
      body: jsonBody({ name: 'Nombre que no debería guardarse' }),
      tests: expectError(403, 'PET_ACCESS_DENIED'),
      examples: [{ name: '403 · Sin acceso de escritura', status: 403, code: 'Forbidden', body: { error: 'You do not have write access to this pet', code: 'PET_ACCESS_DENIED' } }],
    }),
    request({
      name: 'DELETE Borrar mascota ajena → 403',
      method: 'DELETE',
      raw: '{{pets_base}}/pets/{{petId}}',
      description: '`requireOwner` falla: solo el dueño real puede borrar.',
      tests: expectError(403, 'NOT_PET_OWNER'),
      examples: [{ name: '403 · No es el dueño', status: 403, code: 'Forbidden', body: { error: 'This pet does not belong to the specified owner', code: 'NOT_PET_OWNER' } }],
    }),
    request({
      name: 'POST Enviar invitación de prueba (correo mismatch)',
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/invitations',
      description: [
        'Setup del siguiente request: el **dueño** (sobrescribí el auth a `Bearer {{access_token}}`',
        'solo para este request) invita a `{{owner_email}}` (su propio correo, solo para la prueba) y',
        'no a `second_user_email`. Guarda `mismatchInvitationToken`.',
      ].join('\n'),
      auth: bearer('access_token'),
      body: jsonBody({ email: '{{owner_email}}', accessRole: 'VIEWER' }),
      tests: expectStatus(201, [{ variable: 'mismatchInvitationToken', path: 'body.token' }]),
      examples: [
        { name: '201 · Invitación de prueba enviada', status: 201, code: 'Created', body: { id: 'f5a6b7c8-d9e0-4132-8e4f-5a6b7c8d9e0f', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', email: 'owner@example.com', accessRole: 'VIEWER', status: 'PENDING', token: 'b3c4d5e6-f7a8-490a-9e1f-2a3b4c5d6e7f', expiresAt: '2026-11-12T10:40:00', createdAt: '2026-11-05T10:40:00' } },
      ],
    }),
    request({
      name: 'POST Aceptar invitación de otro correo → 403',
      method: 'POST',
      raw: '{{pets_base}}/pets/invitations/{{mismatchInvitationToken}}/accept',
      description: [
        'La invitación de arriba es para `owner_email`, no para el correo de `second_user_token` →',
        '`InvitationEmailMismatchException`.',
      ].join('\n'),
      body: jsonBody({}),
      tests: expectError(403, 'INVITATION_EMAIL_MISMATCH'),
      examples: [{ name: '403 · Correo distinto', status: 403, code: 'Forbidden', body: { error: 'The authenticated email does not match the invitation email', code: 'INVITATION_EMAIL_MISMATCH' } }],
    }),
    request({
      name: 'GET Interno sin key → 401',
      method: 'GET',
      raw: '{{pets_direct_url}}/internal/pets/{{petId}}/owner/{{second_user_id}}',
      description: 'Sin `X-Internal-Api-Key`. `/internal/**` no acepta JWT, así que falla igual con o sin `Authorization`.',
      auth: noAuth,
      tests: expectError(401),
      examples: [{ name: '401 · Sin autenticar', status: 401, code: 'Unauthorized', body: { error: 'Full authentication is required to access this resource' } }],
    }),
    request({
      name: 'GET Interno con key falsa → 401',
      method: 'GET',
      raw: '{{pets_direct_url}}/internal/pets/{{petId}}/owner/{{second_user_id}}',
      description: 'Con un valor de `X-Internal-Api-Key` que no coincide con el configurado en el servicio.',
      auth: noAuth,
      headers: [{ key: 'X-Internal-Api-Key', value: 'clave-invalida-de-prueba' }],
      tests: expectError(401),
      examples: [{ name: '401 · Clave inválida', status: 401, code: 'Unauthorized', body: { error: 'Full authentication is required to access this resource' } }],
    }),
  ],
});

// ---------- 99 · Deprecados ----------

const f99 = folder({
  name: '99 · Deprecados',
  auth: bearer('access_token'),
  description: [
    'Alias `@Deprecated` que siguen funcionando para la app vieja, **sin fecha de eliminación** —',
    'no los uses en código nuevo. Cada uno indica, en su descripción, la ruta oficial que lo',
    'reemplaza.',
  ].join('\n'),
  items: [
    request({
      name: '[DEPRECATED] GET Mascotas de un usuario (ruta vieja)',
      method: 'GET',
      raw: '{{pets_base}}/pets/user/{{second_user_id}}',
      description: [
        'Reemplazada por `GET /pets/me` (carpeta 01). Solo responde si `{userId}` es el `sub` del',
        'token — usá `{{second_user_id}}` con `Bearer {{second_user_token}}`, o tu propio id con',
        '`access_token`, para que no sea un 403.',
      ].join('\n'),
      tests: expectStatus(200),
    }),
    request({
      name: '[DEPRECATED] GET Mascotas compartidas (ruta vieja)',
      method: 'GET',
      raw: '{{pets_base}}/pets/shared',
      description: 'Reemplazada por `GET /pets/shared/me` (carpeta 03). Solo responde si `userId` es el `sub` del token.',
      query: [{ key: 'userId', value: '{{second_user_id}}' }],
      tests: expectStatus(200),
    }),
    request({
      name: '[DEPRECATED] GET ¿Tiene permiso? (ruta vieja)',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/access/{{second_user_id}}',
      description: [
        'Pendiente de mover a `/internal/**`. Solo responde si `{userId}` es el `sub` del token — con',
        '`access_token` usá tu propio id, no `second_user_id`.',
      ].join('\n'),
      tests: expectStatus(200),
    }),
    request({
      name: '[DEPRECATED] GET ¿Es el dueño? (ruta vieja)',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/owner/{{second_user_id}}',
      description: [
        'Reemplazada por `GET /internal/pets/{petId}/owner/{userId}` (carpeta 07) para uso entre',
        'servicios. Solo responde si `{userId}` es el `sub` del token.',
      ].join('\n'),
      tests: expectStatus(200),
    }),
    request({
      name: '[DEPRECATED] GET Invitaciones pendientes por correo (ruta vieja)',
      method: 'GET',
      raw: '{{pets_base}}/pets/invitations/pending',
      description: [
        'Reemplazada por `GET /pets/invitations/me` (carpeta 05). Acepta el header',
        '**`X-User-Email`** o el body `{ "email": "..." }` (dejado abajo), pero solo si coincide con',
        'el correo del token — si no, `403 IDENTITY_MISMATCH`.',
      ].join('\n'),
      auth: bearer('second_user_token'),
      headers: [{ key: 'X-User-Email', value: '{{second_user_email}}', disabled: true }],
      body: jsonBody({ email: '{{second_user_email}}' }),
      tests: expectStatus(200),
    }),
    request({
      name: '[DEPRECATED] POST Aceptar invitación (ruta vieja)',
      method: 'POST',
      raw: '{{pets_base}}/invitations/{{invitationToken}}/accept',
      description: 'Reemplazada por `POST /pets/invitations/{token}/accept` (carpeta 05) — ruta que existía antes de que las invitaciones se movieran bajo `/pets`.',
      auth: bearer('second_user_token'),
      body: jsonBody({}),
      tests: expectStatus(200),
    }),
    request({
      name: '[DEPRECATED] POST Rechazar invitación (ruta vieja)',
      method: 'POST',
      raw: '{{pets_base}}/invitations/{{invitationToken}}/reject',
      description: 'Reemplazada por `POST /pets/invitations/{token}/reject` (carpeta 05) — misma ruta vieja que aceptar.',
      auth: bearer('second_user_token'),
      tests: expectStatus(200),
    }),
  ],
});

// ---------- 06 · Documentos ----------

const f061 = folder({
  name: '06.1 · Generales',
  auth: bearer('access_token'),
  description: [
    'Token: **tuyo** (dueño). El subir genérico va primero para tener un `documentId` con el que',
    'probar el resto; dejá `DELETE Borrar documento` para el final de la subcarpeta.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Subir documento (genérico)',
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/documents',
      description: [
        'Permiso: dueño o EDITOR (`requireWrite`). `multipart/form-data` con **`file`**, **`title`**,',
        '**`documentType`** (ver enum `DocumentType`) y **`description`** opcional. `uploadedBy` es',
        'legado y opcional — el autor real **siempre** es el `sub` del token.',
        '',
        'Válido: no vacío, ≤10MB, tipo en `{pdf, doc, docx, jpeg, png, webp}`.',
        '',
        NOTE_FILE_FIELD,
        '',
        'Guarda `documentId`.',
      ].join('\n'),
      body: formdataBody([
        { key: 'uploadedBy', value: '', description: 'Opcional, legado', disabled: true },
        { key: 'title', value: 'Carné de vacunación' },
        { key: 'description', value: 'Carné de vacunación completo, al día.' },
        { key: 'documentType', value: 'VACCINE' },
        { key: 'file', type: 'file', src: [], description: 'PDF/DOC/DOCX/JPEG/PNG/WEBP ≤ 10MB' },
      ]),
      headers: [],
      tests: expectStatus(201, [{ variable: 'documentId', path: 'body.id' }]),
      examples: [
        {
          name: '201 · Documento subido',
          status: 201,
          code: 'Created',
          body: { id: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', uploadedBy: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', title: 'Carné de vacunación', description: 'Carné de vacunación completo, al día.', fileUrl: 'https://xyz.supabase.co/storage/v1/object/public/pet-documents/pets/9a1c.../documents/e2f3.pdf', mimeType: 'application/pdf', fileSizeBytes: 204800, documentType: 'VACCINE', uploadedAt: '2026-11-05T10:30:00' },
        },
      ],
    }),
    request({
      name: 'GET Listar documentos',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/documents',
      description: 'Permiso: dueño, EDITOR o VIEWER (`requireRead`).',
      tests: expectStatus(200),
      examples: [
        { name: '200 · Un documento', status: 200, body: [{ id: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', title: 'Carné de vacunación', description: 'Carné de vacunación completo, al día.', fileUrl: 'https://xyz.supabase.co/storage/v1/object/public/pet-documents/pets/9a1c.../documents/e2f3.pdf', mimeType: 'application/pdf', fileSizeBytes: 204800, documentType: 'VACCINE', uploadedAt: '2026-11-05T10:30:00' }] },
      ],
    }),
    request({
      name: 'GET Ver un documento',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/documents/{{documentId}}',
      description: [
        'Permiso: dueño, EDITOR o VIEWER (`requireRead`).',
        '',
        `- \`403 DOCUMENT_NOT_IN_PET\` (${CODES.DOCUMENT_NOT_IN_PET}) — el documento existe pero es de otra mascota.`,
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        { name: '200 · Documento', status: 200, body: { id: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', uploadedBy: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9', title: 'Carné de vacunación', description: 'Carné de vacunación completo, al día.', fileUrl: 'https://xyz.supabase.co/storage/v1/object/public/pet-documents/pets/9a1c.../documents/e2f3.pdf', mimeType: 'application/pdf', fileSizeBytes: 204800, documentType: 'VACCINE', uploadedAt: '2026-11-05T10:30:00', active: true } },
      ],
    }),
    request({
      name: 'GET URL de descarga',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/documents/{{documentId}}/download-url',
      description: [
        'Permiso: dueño, EDITOR o VIEWER (`requireRead`) — la URL solo se genera si tenés acceso.',
        '`requestedBy` (query) es legado y opcional, sigue la regla de identidad ("mía"): ausente →',
        'token; igual al token → OK; distinto → `403 IDENTITY_MISMATCH`.',
        '',
        'Hoy devuelve la `fileUrl` pública tal cual (no genera todavía una URL firmada temporal).',
      ].join('\n'),
      query: [{ key: 'requestedBy', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(200),
      examples: [
        { name: '200 · URL de descarga', status: 200, body: { documentId: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', title: 'Carné de vacunación', downloadUrl: 'https://xyz.supabase.co/storage/v1/object/public/pet-documents/pets/9a1c.../documents/e2f3.pdf', mimeType: 'application/pdf', fileSizeBytes: 204800 } },
      ],
    }),
    request({
      name: 'PATCH Editar metadatos',
      method: 'PATCH',
      raw: '{{pets_base}}/pets/{{petId}}/documents/{{documentId}}/metadata',
      description: [
        'Permiso: el dueño, o el EDITOR **solo si él mismo subió este documento**',
        '(`requireOwnerOrUploader`) — un EDITOR que subió el documento y después fue bajado a VIEWER',
        '**pierde** este permiso. VIEWER nunca, sin excepción.',
        '',
        'Solo `title`/`description`; al menos uno no vacío (`400` si ambos vienen vacíos/nulos).',
      ].join('\n'),
      body: jsonBody({ title: 'Carné de vacunación (actualizado)' }),
      tests: expectStatus(200),
      examples: [
        { name: '200 · Metadatos actualizados', status: 200, body: { id: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', title: 'Carné de vacunación (actualizado)', description: 'Carné de vacunación completo, al día.', documentType: 'VACCINE', uploadedAt: '2026-11-05T10:30:00' } },
      ],
    }),
    request({
      name: 'DELETE Borrar documento',
      method: 'DELETE',
      raw: '{{pets_base}}/pets/{{petId}}/documents/{{documentId}}',
      description: [
        'Mismo permiso que editar metadatos: dueño, o EDITOR solo si él lo subió',
        '(`requireOwnerOrUploader`). `requestedBy` (query) legado y opcional, regla de identidad',
        '"mía".',
        '',
        '⚠️ Corré este request al final de `06.1`: borra el documento que usan `GET`/`PATCH` de',
        'arriba.',
      ].join('\n'),
      query: [{ key: 'requestedBy', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(200),
      examples: [
        { name: '200 · Documento eliminado', status: 200, body: { documentId: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', message: 'Document deleted successfully' } },
      ],
    }),
  ],
});

const f062 = folder({
  name: '06.2 · Subir por tipo',
  auth: bearer('access_token'),
  description: [
    'Un `POST /pets/{petId}/documents` listo por cada `DocumentType` del enum, para no editar el',
    'form-data a mano. Mismo endpoint y permisos que `06.1 · POST Subir documento (genérico)`',
    '(dueño o EDITOR; `requireWrite`).',
    '',
    'Cada uno guarda `documentId_<TIPO>` para usarlo en `06.3 · Listar por tipo`.',
  ].join('\n'),
  items: DOC_TYPES.map(({ type, label, fileName, mime }) =>
    request({
      name: `POST Subir ${label}`,
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/documents',
      description: [
        `Sube un documento de tipo **\`${type}\`** (${label}).`,
        '',
        '`multipart/form-data` con **`file`**, **`title`** y `documentType` fijo en `' + type + '`.',
        '`description` opcional. `uploadedBy` legado y opcional (el autor real es siempre el `sub`',
        'del token).',
        '',
        NOTE_FILE_FIELD,
      ].join('\n'),
      body: formdataBody([
        { key: 'uploadedBy', value: '', description: 'Opcional, legado', disabled: true },
        { key: 'title', value: `Documento de ${label}` },
        { key: 'description', value: `Registro de ${label} de la mascota.` },
        { key: 'documentType', value: type, description: `Fijo en ${type}` },
        { key: 'file', type: 'file', src: [], description: `${mime} · ${fileName}` },
      ]),
      headers: [],
      tests: expectStatus(201, [{ variable: `documentId_${type}`, path: 'body.id' }]),
      examples: [
        {
          name: `201 · ${label} subido`,
          status: 201,
          code: 'Created',
          body: {
            id: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70',
            petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b',
            uploadedBy: '7e45c6d7-8e9f-4a01-b2c3-d4e5f6a7b8c9',
            title: `Documento de ${label}`,
            description: `Registro de ${label} de la mascota.`,
            fileUrl: `https://xyz.supabase.co/storage/v1/object/public/pet-documents/pets/9a1c.../documents/e2f3.pdf`,
            mimeType: mime,
            fileSizeBytes: 204800,
            documentType: type,
            uploadedAt: '2026-11-05T10:30:00',
          },
        },
      ],
    })
  ),
});

const f063 = folder({
  name: '06.3 · Listar por tipo',
  auth: bearer('access_token'),
  description: [
    'Un `GET /pets/{petId}/documents/type/{documentType}` por cada tipo. Permiso: dueño, EDITOR o',
    'VIEWER (`requireRead`). Correlo después de `06.2` para ver el documento subido en cada tipo.',
  ].join('\n'),
  items: DOC_TYPES.map(({ type, label }) =>
    request({
      name: `GET Listar ${label}`,
      method: 'GET',
      raw: `{{pets_base}}/pets/{{petId}}/documents/type/${type}`,
      description: `Lista los documentos de tipo \`${type}\` (${label}) de la mascota.`,
      tests: expectStatus(200),
      examples: [
        {
          name: `200 · Un documento de ${label}`,
          status: 200,
          body: [
            { id: 'e2f3a4b5-c6d7-4e89-0f1a-2b3c4d5e6f70', title: `Documento de ${label}`, description: `Registro de ${label} de la mascota.`, fileUrl: 'https://xyz.supabase.co/storage/v1/object/public/pet-documents/pets/9a1c.../documents/e2f3.pdf', mimeType: 'application/pdf', fileSizeBytes: 204800, documentType: type, uploadedAt: '2026-11-05T10:30:00' },
          ],
        },
      ],
    })
  ),
});

const f06 = folder({
  name: '06 · Documentos',
  description: 'Documentos clínicos de la mascota (vacunas, exámenes, etc.), guardados en el bucket `pet-documents` de Supabase.',
  items: [f061, f062, f063],
});

// ---------- 04 · Invitaciones — lado del dueño ----------

const f04 = folder({
  name: '04 · Invitaciones — lado del dueño',
  auth: bearer('access_token'),
  description: [
    'Token: **tuyo** (dueño). Invitar por correo a alguien que todavía no tiene cuenta vinculada a',
    'la mascota.',
    '',
    '⚠️ `POST Reenviar` **invalida** el token de la invitación anterior (cancela la vieja y crea una',
    'nueva). Si vas a probar `05 · POST Rechazar` después de `05 · POST Aceptar`, volvé a correr',
    '`POST Reenviar` primero para tener un token sin usar.',
  ].join('\n'),
  items: [
    request({
      name: 'POST Enviar invitación',
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/invitations',
      description: [
        'Permiso: **solo el dueño**. `ownerId` en el body es legado y opcional. Rechaza si ya hay una',
        'invitación `PENDING` para esa mascota + correo (`409`).',
        '',
        'El correo debe ser el de `second_user_email` para que la segunda cuenta pueda verla y',
        'aceptarla en la carpeta 05.',
        '',
        'Guarda `invitationToken`.',
      ].join('\n'),
      body: jsonBody({ email: '{{second_user_email}}', accessRole: 'VIEWER' }),
      tests: expectStatus(201, [{ variable: 'invitationToken', path: 'body.token' }, { variable: 'invitationId', path: 'body.id' }]),
      examples: [
        {
          name: '201 · Invitación enviada',
          status: 201,
          code: 'Created',
          body: { id: 'c3d4e5f6-a7b8-4910-8c2d-3e4f5a6b7c8d', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', email: 'segunda.cuenta@example.com', accessRole: 'VIEWER', status: 'PENDING', token: 'f1e2d3c4-b5a6-4798-8c0d-1e2f3a4b5c6d', expiresAt: '2026-11-12T10:00:00', createdAt: '2026-11-05T10:00:00' },
        },
      ],
    }),
    request({
      name: 'POST Reenviar invitación',
      method: 'POST',
      raw: '{{pets_base}}/pets/{{petId}}/invitations/resend',
      description: [
        'Permiso: **solo el dueño**. Cancela todas las invitaciones `PENDING` de esa mascota + correo',
        'y crea una nueva (token y vencimiento nuevos). `ownerId` legado, opcional.',
        '',
        'Sobrescribe `invitationToken`.',
      ].join('\n'),
      body: jsonBody({ email: '{{second_user_email}}', accessRole: 'VIEWER' }),
      tests: expectStatus(200, [{ variable: 'invitationToken', path: 'body.token' }, { variable: 'invitationId', path: 'body.id' }]),
      examples: [
        {
          name: '200 · Invitación reenviada',
          status: 200,
          body: { id: 'd4e5f6a7-b8c9-4a21-9d3e-4f5a6b7c8d9e', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', email: 'segunda.cuenta@example.com', accessRole: 'VIEWER', status: 'PENDING', token: 'a2b3c4d5-e6f7-4809-9d1e-2f3a4b5c6d7e', expiresAt: '2026-11-12T10:20:00', createdAt: '2026-11-05T10:20:00' },
        },
      ],
    }),
    request({
      name: 'GET Invitaciones pendientes de la mascota',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/invitations/pending',
      description: 'Permiso: **solo el dueño**. `ownerId` (query) opcional, misma regla de identidad.',
      query: [{ key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(200),
      examples: [
        { name: '200 · Una pendiente', status: 200, body: [{ id: 'd4e5f6a7-b8c9-4a21-9d3e-4f5a6b7c8d9e', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', email: 'segunda.cuenta@example.com', accessRole: 'VIEWER', status: 'PENDING', token: 'a2b3c4d5-e6f7-4809-9d1e-2f3a4b5c6d7e', expiresAt: '2026-11-12T10:20:00', createdAt: '2026-11-05T10:20:00' }] },
      ],
    }),
    request({
      name: 'GET ¿Ya está invitado?',
      method: 'GET',
      raw: '{{pets_base}}/pets/{{petId}}/invitations/check',
      description: [
        'Permiso: **solo el dueño**. `ownerId` (query) opcional, misma regla de identidad que el',
        'resto de esta carpeta.',
      ].join('\n'),
      query: [
        { key: 'email', value: '{{second_user_email}}' },
        { key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true },
      ],
      tests: expectStatus(200),
      examples: [{ name: '200 · Ya invitado', status: 200, body: { invited: true } }],
    }),
  ],
});

// ---------- 05 · Invitaciones — lado del invitado ----------

const f05 = folder({
  name: '05 · Invitaciones — lado del invitado',
  auth: bearer('second_user_token'),
  description: [
    'Token: **de la segunda cuenta** (la invitada). Requiere haber enviado una invitación a',
    '`second_user_email` en la carpeta 04.',
    '',
    'Para aceptar/rechazar, el correo del token debe ser **exactamente** el de la invitación (sin',
    'distinguir mayúsculas) y estar **verificado** — ver `03 · INVITATION_EMAIL_MISMATCH` /',
    '`EMAIL_NOT_VERIFIED` en la carpeta 08.',
  ].join('\n'),
  items: [
    request({
      name: 'GET Mis invitaciones pendientes',
      method: 'GET',
      raw: '{{pets_base}}/pets/invitations/me',
      description: 'Invitaciones `PENDING` para el correo del token. Oficial; sustituye al alias deprecado `GET /pets/invitations/pending` (carpeta 99).',
      tests: expectStatus(200),
      examples: [
        { name: '200 · Una pendiente', status: 200, body: [{ id: 'd4e5f6a7-b8c9-4a21-9d3e-4f5a6b7c8d9e', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', email: 'segunda.cuenta@example.com', accessRole: 'VIEWER', status: 'PENDING', token: 'a2b3c4d5-e6f7-4809-9d1e-2f3a4b5c6d7e', expiresAt: '2026-11-12T10:20:00', createdAt: '2026-11-05T10:20:00' }] },
      ],
    }),
    request({
      name: 'GET ¿Expiró la invitación?',
      method: 'GET',
      raw: '{{pets_base}}/pets/invitations/{{invitationToken}}/expired',
      description: 'No requiere relación con la mascota, solo el token de la invitación en la ruta.',
      tests: expectStatus(200),
      examples: [{ name: '200 · Vigente', status: 200, body: { expired: false } }],
    }),
    request({
      name: 'POST Aceptar invitación',
      method: 'POST',
      raw: '{{pets_base}}/pets/invitations/{{invitationToken}}/accept',
      description: [
        'Identidad = `sub` del token (`userId` en el body es legado, opcional, sigue la regla',
        '"mía"). Crea el `PetUserAccess` con el rol de la invitación.',
        '',
        `- \`403 INVITATION_EMAIL_MISMATCH\` (${CODES.INVITATION_EMAIL_MISMATCH}) — el correo del token no es el de la invitación.`,
        `- \`403 EMAIL_NOT_VERIFIED\` (${CODES.EMAIL_NOT_VERIFIED}) — el correo del token no está verificado.`,
        '- `400 InvitationAlreadyProcessedException` — ya fue aceptada/rechazada o venció.',
        `- \`409 ACCESS_ALREADY_GRANTED\` — ya tenías acceso a esta mascota.`,
      ].join('\n'),
      body: jsonBody({}),
      tests: expectStatus(200),
      examples: [
        { name: '200 · Invitación aceptada', status: 200, body: { invitationId: 'd4e5f6a7-b8c9-4a21-9d3e-4f5a6b7c8d9e', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', userId: '{{second_user_id}}', accessRole: 'VIEWER', status: 'ACCEPTED' } },
      ],
    }),
    request({
      name: 'POST Rechazar invitación',
      method: 'POST',
      raw: '{{pets_base}}/pets/invitations/{{invitationToken}}/reject',
      description: [
        'Mismas validaciones de correo que aceptar (email match + verificado), sin `userId`.',
        '',
        '⚠️ Si ya usaste `{{invitationToken}}` para aceptar arriba, este request falla con',
        '`InvitationAlreadyProcessedException` (ya no está `PENDING`). Volvé a `04 · POST Reenviar',
        'invitación` para conseguir un token sin usar antes de probar un rechazo real.',
      ].join('\n'),
      tests: expectStatus(200),
      examples: [
        { name: '200 · Invitación rechazada', status: 200, body: { invitationId: 'd4e5f6a7-b8c9-4a21-9d3e-4f5a6b7c8d9e', petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', status: 'REJECTED' } },
      ],
    }),
  ],
});

// ---------- 02 · Foto de la mascota ----------

const f02 = folder({
  name: '02 · Foto de la mascota',
  auth: bearer('access_token'),
  description: 'Token: dueño o EDITOR (`requireWrite`). Sube/borra la foto de perfil en el bucket `pet-photos` de Supabase.',
  items: [
    request({
      name: 'PATCH Subir foto',
      method: 'PATCH',
      raw: '{{pets_base}}/pets/{{petId}}/photo',
      description: [
        '`multipart/form-data` con el campo **`file`** (imagen). `ownerId` es legado y opcional,',
        'sigue la misma regla de identidad que editar mascota (tuyo o el dueño real → OK; otro → 403',
        'IDENTITY_MISMATCH).',
        '',
        NOTE_FILE_FIELD,
      ].join('\n'),
      body: formdataBody([
        { key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true },
        { key: 'file', type: 'file', src: [], description: 'Imagen de la mascota' },
      ]),
      headers: [],
      tests: expectStatus(200),
      examples: [
        {
          name: '200 · Foto subida',
          status: 200,
          body: { petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', photoUrl: 'https://xyz.supabase.co/storage/v1/object/public/pet-photos/pets/9a1c.../profile.jpg', message: 'Photo uploaded successfully' },
        },
      ],
    }),
    request({
      name: 'DELETE Borrar foto',
      method: 'DELETE',
      raw: '{{pets_base}}/pets/{{petId}}/photo',
      description: '`ownerId` (query) opcional, misma regla de identidad.',
      query: [{ key: 'ownerId', value: '', description: 'Opcional, legado', disabled: true }],
      tests: expectStatus(200),
      examples: [
        { name: '200 · Foto eliminada', status: 200, body: { petId: '9a1c2f3e-4b5d-6e7f-8a9b-0c1d2e3f4a5b', message: 'Photo deleted successfully' } },
      ],
    }),
  ],
});

// ---------- Colección ----------

const collection = {
  info: {
    _postman_id: 'a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
    name: 'MyAnimaLog · Pets Service',
    description: [
      '# MyAnimaLog · Pets Service',
      '',
      'Colección completa del `pet-service` (**37 endpoints**, todos los del código, incluidos los',
      '`/internal/**` y los alias deprecados). Generada desde los controladores con',
      '`postman/build-collection.js` y verificada con `postman/check-coverage.js`.',
      '',
      '## Cómo empezar',
      '',
      '1. Importá la colección y **uno** de los dos environments (`Local` o `Cloud Run`).',
      '2. Llená en el environment: `owner_email`/`owner_password` (primera cuenta, dueña de la',
      '   mascota de prueba) y `second_user_email`/`second_user_password` (segunda cuenta, con',
      '   correo **verificado**, para EDITOR/VIEWER y para las pruebas de permisos).',
      '3. **00 · Login** → `access_token`. **00 · Login (segunda cuenta)** → `second_user_token`.',
      '   **00 · Mi usuario (segunda cuenta)** → `second_user_id`.',
      '4. **01 · Crear mascota** → `petId`. De ahí en adelante, en orden: 01 (menos el borrar) → 02 →',
      '   03 → 04 → 05 → 06 → 07 → 08 → 99, y por último `01 · DELETE Borrar mascota`.',
      '',
      '## Qué token usa cada carpeta',
      '',
      '| Carpeta | Token |',
      '|---|---|',
      '| 00 Autenticación | ninguno (son del User service) |',
      '| 01 Mis mascotas | `access_token` (dueño) |',
      '| 02 Foto de la mascota | `access_token` (dueño o EDITOR) |',
      '| 03 Compartir y accesos | `access_token`, salvo `GET /pets/shared/me` (cambialo a mano a `second_user_token` para verlo desde el invitado) |',
      '| 04 Invitaciones — dueño | `access_token` |',
      '| 05 Invitaciones — invitado | `second_user_token` |',
      '| 06 Documentos | `access_token` (dueño o EDITOR, salvo lectura que también admite VIEWER) |',
      '| 07 Internos | ninguno; header `X-Internal-Api-Key` |',
      '| 08 Pruebas de permisos | `second_user_token` (usuario ajeno) |',
      '| 99 Deprecados | `access_token`, salvo donde se indica |',
      '',
      'La autenticación por defecto de la colección es `Bearer {{access_token}}`; cada carpeta la',
      'sobrescribe donde hace falta.',
      '',
      '## Matriz de permisos (dueño / EDITOR / VIEWER)',
      '',
      '| Acción | OWNER | EDITOR | VIEWER |',
      '|---|---|---|---|',
      '| Ver mascota, edad, documentos, descargar | ✅ | ✅ | ✅ |',
      '| Editar mascota, foto | ✅ | ✅ | ❌ |',
      '| Subir documento | ✅ | ✅ | ❌ |',
      '| Editar/borrar documento que **él mismo** subió | ✅ | ✅ | ❌ |',
      '| Editar/borrar documento subido por otra persona | ✅ | ❌ | ❌ |',
      '| Borrar mascota, compartir, invitar, cambiar rol, revocar | ✅ | ❌ | ❌ |',
      '',
      'Si un EDITOR que subió un documento es bajado a VIEWER, **pierde** la capacidad de editarlo o',
      'borrarlo (el permiso depende del rol vigente, no de haber sido el autor original) — ver',
      '`03 · PATCH Cambiar rol` y la nota de `06.1 · PATCH Editar metadatos`.',
      '',
      '## Probar EDITOR/VIEWER con la segunda cuenta',
      '',
      'La carpeta **03** otorga a `second_user_id` el rol EDITOR y después lo cambia a VIEWER. Para',
      'repetir cualquier request de las carpetas 01/06 **como esa cuenta**, cambiá temporalmente el',
      'auth del request a `Bearer {{second_user_token}}` (sin tocar la carpeta entera, para no',
      'romper el resto del flujo).',
      '',
      '## Variables de URL',
      '',
      '`{{pets_base}}` es la base de las rutas **públicas** de Pets — puede ser el servicio directo o',
      'el gateway (que recibe `/api/pets/...` y le quita el `/api`). En Local viene apuntando directo',
      'al servicio; para probar por el gateway, cambiá el valor de `pets_base` a',
      '`{{gateway_url}}/api`.',
      '',
      '`{{pets_direct_url}}` es **siempre** el servicio directo, nunca el gateway — la usa la carpeta',
      '07 porque `/internal/**` no se expone por el gateway.',
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  auth: bearer('access_token'),
  item: [auth00, f01, f02, f03, f04, f05, f06, f07, f08, f99],
};

fs.writeFileSync(
  'postman/MyAnimaLogPets.postman_collection.json',
  JSON.stringify(collection, null, 2) + '\n',
  'utf8'
);

const count = (items) =>
  items.reduce((acc, i) => acc + (i.item ? count(i.item) : 1), 0);

console.log('postman/MyAnimaLogPets.postman_collection.json escrito');
console.log(`Carpetas: ${collection.item.length} · Requests: ${count(collection.item)}`);

