# Colección de Postman — `pet-service`

Cubre los **37 endpoints** del servicio (todos los del código, incluidos los `/internal/**` y los
deprecados), más 5 requests del User service para conseguir tokens.

| Archivo | Qué es |
|---|---|
| `MyAnimaLogPets.postman_collection.json` | La colección (v2.1). **Generada**, no editar a mano. |
| `MyAnimaLogPets-Local.postman_environment.json` | Environment para correr todo en tu máquina. |
| `MyAnimaLogPets-CloudRun.postman_environment.json` | Environment para Cloud Run (URLs a reemplazar). |
| `build-collection.js` | Genera la colección. Es el archivo que **sí** se edita. |
| `check-coverage.js` | Compara los controladores con la colección. Debe dar 0 faltantes y 0 sobrantes. |

## Importar

1. Postman → **Import** → arrastrá los tres `.json` (la colección y los dos environments).
2. Arriba a la derecha, seleccioná el environment (**Local** o **Cloud Run**).
3. Llená los valores de abajo.

## Qué llenar en cada environment

**Local** ya viene con las URLs puestas; solo faltan las credenciales:

| Variable | Qué poner |
|---|---|
| `owner_email` / `owner_password` | Primera cuenta: será la **dueña** de la mascota de prueba. |
| `second_user_email` / `second_user_password` | Segunda cuenta, con **correo verificado** — hace de EDITOR/VIEWER (carpeta 03) y de "usuario ajeno" en las pruebas de permisos (carpeta 08) y en el lado del invitado (carpeta 05). |
| `internal_api_key` | El mismo `INTERNAL_API_KEY` del `.env` del servicio. Solo para la carpeta 07 y los dos últimos requests de la 08. |

**Cloud Run**: además de lo anterior, reemplazá las URLs que dicen `REEMPLAZAR` (`pets_direct_url`,
`gateway_url`, `user_url`; `pets_base` ya apunta al gateway, ver abajo).

### `pets_base` vs. `pets_direct_url`

- **`pets_base`** es la base de las rutas **públicas** de Pets (todo salvo `/internal/**`). En
  `Local` apunta directo al servicio (`http://localhost:8081`). Para probar a través del gateway
  (como lo usa el frontend — el gateway recibe `/api/pets/...` y le quita el `/api`), cambiá su
  valor a `{{gateway_url}}/api`. En `Cloud Run` ya viene apuntando al gateway, porque así es como lo
  usa el frontend en ese ambiente.
- **`pets_direct_url`** es **siempre** el servicio directo, nunca el gateway. La usa la carpeta
  **07 · Internos** (y los dos requests de `/internal` en la 08): `/internal/**` **nunca** se expone
  por el gateway.

### Valores que se llenan solos

Los scripts de test guardan estas variables al vuelo, no hay que escribirlas: `access_token`,
`refresh_token`, `second_user_token`, `second_user_id`, `petId`, `accessId`, `invitationId`,
`invitationToken`, `mismatchInvitationToken`, `documentId` y `documentId_<TIPO>` (uno por cada
`DocumentType`: `VACCINE`, `DEWORMING`, `LAB_RESULT`, `MEDICAL_RECORD`, `PRESCRIPTION`,
`SURGERY_REPORT`, `ALLERGY_REPORT`, `OTHER`).

`targetUserId` se llena a mano solo si querés probar `03 · POST Dar acceso (rol VIEWER)` con una
tercera cuenta distinta de `second_user_id` (si no, ese request puede saltarse).

Todas las variables de token y contraseña son de tipo `secret` y vienen **vacías**: los archivos de
este repo no contienen ningún secreto real, y Postman las oculta al compartir.

## Orden de prueba recomendado

```
00 · Login                              → access_token
00 · Login (segunda cuenta)             → second_user_token
00 · GET Mi usuario (segunda cuenta)    → second_user_id
01 · POST Crear mascota                 → petId
01 · (el resto, menos el borrar)
02 · Foto de la mascota
03 · Compartir y accesos                → otorga EDITOR a second_user_id, lo baja a VIEWER, revoca
04 · Invitaciones — lado del dueño      → invitationToken
05 · Invitaciones — lado del invitado   (con second_user_token)
06 · Documentos (06.1 → 06.2 → 06.3)
07 · Internos                            (header X-Internal-Api-Key)
08 · Pruebas de permisos                 (con second_user_token, debe fallar todo)
99 · Deprecados
01 · DELETE Borrar mascota               ← al final, no antes
```

La carpeta **03** deja a `second_user_id` **sin** acceso a la mascota al terminar (otorga, cambia de
rol y revoca) — así la carpeta **08** lo puede usar como un usuario realmente ajeno.

La carpeta **04 · POST Reenviar invitación** invalida el token de la invitación anterior. Si querés
probar `05 · POST Rechazar invitación` después de haber usado `05 · POST Aceptar invitación`, volvé
a `04 · POST Reenviar invitación` primero para tener un token sin usar (una invitación ya `ACCEPTED`
no se puede rechazar).

## Matriz de permisos (dueño / EDITOR / VIEWER)

| Acción | OWNER | EDITOR | VIEWER |
|---|---|---|---|
| Ver mascota, edad, documentos, descargar | ✅ | ✅ | ✅ |
| Editar mascota, foto | ✅ | ✅ | ❌ |
| Subir documento | ✅ | ✅ | ❌ |
| Editar/borrar documento que **él mismo** subió | ✅ | ✅ | ❌ |
| Editar/borrar documento subido por otra persona | ✅ | ❌ | ❌ |
| Borrar mascota, compartir, invitar, cambiar rol, revocar | ✅ | ❌ | ❌ |

Si un EDITOR que subió un documento es bajado a VIEWER, **pierde** la capacidad de editarlo o
borrarlo — el permiso depende del rol vigente, no de haber sido el autor original. Ver
`docs/integracion/PETS_CAMBIOS_PARA_FRONTEND.md` y `CLAUDE.md` para el detalle completo (incluidos
todos los `code` de error).

## Regenerar y verificar la colección

La colección se genera para que la estructura repetitiva (auth por carpeta, scripts, ejemplos) no se
desincronice. Si agregás o cambiás un endpoint:

```bash
node postman/build-collection.js          # reescribe el .json de la colección
node postman/check-coverage.js            # 0 faltantes, 0 sobrantes
node postman/check-coverage.js --table    # + tabla carpeta / método / ruta
```

`check-coverage.js` lee los controladores de `src/main/java` (clase `@RestController` →
`@RequestMapping` como base + cada `@*Mapping`) y los compara con los `url.raw` de la colección,
normalizando las path variables **y** los valores fijos de `DocumentType` que usan los requests "por
tipo" (`06.2`/`06.3`), para que cuenten como el mismo endpoint con `{documentType}` del código. Los
requests del User service (`{{user_url}}`, carpeta 00) se ignoran porque no son endpoints de este
servicio. Los requests "por tipo" y los de la carpeta 08 cuentan como **variantes** de un endpoint
ya cubierto, no como sobrantes.

Si agregás un endpoint y te olvidás de la colección, `check-coverage.js` falla con exit code 1 y te
dice exactamente qué falta.

## Advertencias

- **La carpeta 00 es del User service**, no de Pets, y sus rutas (`/auth/local`, `/auth/refresh`,
  `/auth/me`) **no están verificadas** contra el código del User service (este repo es solo Pets).
  Si no coinciden, ajustalas — no afectan la cobertura.
- **La carpeta 07 (y los dos `/internal` de la 08) nunca deben pasar por el gateway.** Son servicio
  a servicio, con `X-Internal-Api-Key` y sin JWT — por eso usan `{{pets_direct_url}}` en vez de
  `{{pets_base}}`.
- **La carpeta 99 son endpoints deprecados** sin fecha de eliminación. No los uses en código nuevo;
  cada uno indica en su descripción la ruta oficial que lo reemplaza.
- Los requests que suben archivos (foto, documentos) tienen el campo `file` vacío: hay que elegir el
  archivo a mano en Postman (**"Select files"**) antes de enviarlos — la ruta del ejemplo no existe
  en otra computadora.
- `01 · DELETE Borrar mascota` borra en cascada los documentos y accesos de la mascota de prueba:
  dejalo para el final.
