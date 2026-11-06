# Pets: cambios de autenticación para el frontend

> Ramas `feature/pets-jwks-y-endpoints-internos` (PETS-01 y PETS-04) y `feature/pets-02-identidad-desde-token` (PETS-02). Pendiente de despliegue.

## 1. Qué cambió

Antes, el servicio de Pets no validaba nada: `SecurityConfig` tenía `anyRequest().permitAll()` y
cualquier petición pasaba sin token. **Ahora todas las rutas exigen un JWT válido**, salvo Swagger y
los endpoints de salud.

En la práctica: toda llamada a Pets debe ir con el header

```
Authorization: Bearer <access_token>
```

- El token es el **`access_token` RS256 que emite el User service** (el mismo que ya se usa con los
  demás microservicios). Pets solo lo verifica contra el JWKS del User service; no emite tokens.
- Se valida la firma (RS256 vía JWKS), el emisor (`iss = myanimalog-user-service`) y la audiencia
  (`aud = myanimalog-api`). Vida del token: 15 minutos, así que hay que refrescarlo como con los
  otros servicios.
- **Si hay gateway, el token se envía igual que a los demás servicios**: el gateway no cambia; cada
  servicio valida el JWT por sí mismo (CONTRATOS_COMPARTIDOS.md §5).

Respuestas de error, con el formato de error del servicio (`{"error": "..."}`):

| Código | Cuándo |
|---|---|
| `401` | Falta el token, está vencido, la firma es inválida, o el `iss`/`aud` no coinciden |
| `403` | Token válido pero sin permiso para ese recurso |
| `404` | Ruta inexistente (antes devolvía `500`) |
| `405` | Método no permitido para esa ruta (antes `500`) |
| `400` | Cuerpo JSON inválido o UUID mal formado en la ruta (antes `500`) |

## 2. Rutas públicas (sin token)

Solo estas:

- `GET /swagger-ui.html`, `/swagger-ui/**`, `/api-docs/**`
- `GET /actuator/health`, `GET /actuator/info`

## 3. Endpoints que ahora requieren `Authorization: Bearer`

Es decir, **todos** los de abajo. Ninguno funcionaba con token antes y ninguno funciona sin token
ahora.

### Mascotas

| Método | Ruta |
|---|---|
| POST | `/pets` |
| GET | `/pets/{petId}` |
| PATCH | `/pets/{petId}` |
| DELETE | `/pets/{petId}` |
| GET | `/pets/{petId}/age` |
| GET | `/pets/user/{userId}` |
| PATCH | `/pets/{petId}/photo` |
| DELETE | `/pets/{petId}/photo` |

### Compartir y accesos

| Método | Ruta |
|---|---|
| POST | `/pets/{petId}/access` |
| PATCH | `/pets/{petId}/access/{targetUserId}` |
| DELETE | `/pets/{petId}/access/{targetUserId}` |
| GET | `/pets/{petId}/shared` |
| GET | `/pets/shared` |
| GET | `/pets/{petId}/access/{userId}` (**deprecado**, ver §4) |
| GET | `/pets/{petId}/owner/{userId}` (**deprecado**, ver §4) |

### Invitaciones

| Método | Ruta |
|---|---|
| POST | `/pets/{petId}/invitations` |
| POST | `/pets/{petId}/invitations/resend` |
| GET | `/pets/{petId}/invitations/pending` |
| GET | `/pets/invitations/pending` |
| GET | `/pets/{petId}/invitations/check` |
| GET | `/pets/invitations/{token}/expired` |
| POST | `/pets/invitations/{token}/accept` |
| POST | `/pets/invitations/{token}/reject` |

> **Corrección sobre la versión anterior de este documento:** las rutas de aceptar/rechazar son
> `/pets/invitations/{token}/accept` y `/pets/invitations/{token}/reject` (con el prefijo `/pets`).
> `/invitations/{token}/accept` y `/invitations/{token}/reject` (sin `/pets`) siguen funcionando
> pero son el alias **deprecado** — ver §5.6.
>
> Ojo con `GET /pets/invitations/{token}/expired` y los `POST .../accept` / `.../reject`: si la
> app abre el enlace del correo **antes** de iniciar sesión, ahora recibirá `401`. El flujo debe
> pedir login primero y luego llamar con el token del usuario — ver §5.3 para el detalle completo.

### Documentos

| Método | Ruta |
|---|---|
| POST | `/pets/{petId}/documents` |
| GET | `/pets/{petId}/documents` |
| GET | `/pets/{petId}/documents/type/{documentType}` |
| GET | `/pets/{petId}/documents/{documentId}` |
| GET | `/pets/{petId}/documents/{documentId}/download-url` |
| PATCH | `/pets/{petId}/documents/{documentId}/metadata` |
| DELETE | `/pets/{petId}/documents/{documentId}` |

## 4. Endpoints deprecados: `IsOwner` y `HasPermission`

`GET /pets/{petId}/owner/{userId}` y `GET /pets/{petId}/access/{userId}` eran públicos y permitían
consultar las relaciones **de cualquier usuario**. Ahora:

- Exigen token.
- Solo responden si `{userId}` es el `sub` del token. Consultar a otro usuario devuelve **403**.
- Están marcados `@Deprecated`. La versión para servicios es
  `GET /internal/pets/{petId}/owner/{userId}` (solo con `X-Internal-Api-Key`, no se expone por el
  gateway).

Si el frontend los usa para saber si la sesión actual es dueña de una mascota, seguirán funcionando
mientras pase su propio `sub`. Aun así, conviene dejar de depender de ellos.

Cambio de comportamiento: `GET /pets/{petId}/owner/{userId}` con una mascota inexistente ahora
devuelve `200 {"owner": false}` en vez de `404`.

## 5. PETS-02: la identidad siempre sale del token

A partir de esta rama, quién eres para efectos de autorización **siempre** es el `sub` (y el
`email`/`email_verified`) del JWT — nunca lo que venga en el body, en un query param o en la ruta.
Los campos viejos (`ownerId`, `userId`, `uploadedBy`, `requestedBy`, `targetUserId`, el header
`X-User-Email`) **ya no se usan para decidir permisos**. Se mantienen por compatibilidad mientras
la app vieja los siga enviando, pero ahora solo sirven para una validación de coherencia.

### 5.1 La regla de compatibilidad, en simple

Si tu app sigue mandando esos campos, **no tienes que cambiar nada para que siga funcionando**,
siempre que mandes el valor correcto:

- **Si no mandas el campo**: se usa tu `sub`/`email` del token. Funciona.
- **Si mandas tu propio `sub`/email** (el de la sesión con la que hiciste login): funciona igual
  que antes.
- **Caso especial — endpoints sobre "una mascota" (editar, borrar, foto, compartir, invitaciones,
  documentos)**: si tu app todavía manda el **`ownerId`/`uploadedBy` real de la mascota** (el dueño
  verdadero, no el tuyo) porque así lo hacía antes para que un EDITOR pudiera actuar sobre una
  mascota que no es suya, **también funciona** — se acepta si coincide con tu `sub` **o** con el
  dueño real de esa mascota. Esto no te da permisos extra: si tu rol real es VIEWER, seguirás
  recibiendo 403 aunque mandes el `ownerId` correcto.
- **Si mandas un id/email que no es ni el tuyo ni (cuando aplica) el del dueño real**: `403` con
  `code: "IDENTITY_MISMATCH"`. Esto solo puede pasar si la app está mal armada (por ejemplo, un bug
  que mezcla el usuario de otra sesión) — no debería ocurrir en uso normal.
- **Endpoints sobre "lo mío"** (`/pets/user/{userId}`, `/pets/shared?userId=`, las variantes de
  invitaciones pendientes por email): aquí **no** aplica la excepción del dueño real — solo se
  acepta tu propio id/email, nunca el de otra persona, ni siquiera el "real" de algo.

### 5.2 Matriz de permisos (dueño / EDITOR / VIEWER)

| Acción | OWNER | EDITOR | VIEWER |
|---|---|---|---|
| Ver mascota, edad, listar/ver documentos, descargar documento | ✅ | ✅ | ✅ |
| Editar datos de la mascota, foto de perfil | ✅ | ✅ | ❌ |
| Subir documento | ✅ | ✅ | ❌ |
| Editar metadata / borrar documento **que él mismo subió** | ✅ | ✅ | ❌ |
| Editar metadata / borrar documento subido por **otra persona** (incluido el dueño) | ✅ | ❌ | ❌ |
| Borrar mascota, compartir, cambiar rol, revocar acceso, invitaciones | ✅ | ❌ | ❌ |

**Importante:** si un EDITOR sube un documento y luego el dueño le baja el rol a VIEWER, pierde la
capacidad de editar/borrar ese documento — el permiso depende del rol **actual**, no de haber sido
quien lo subió en su momento.

### 5.3 Invitaciones: correo y verificación

Para **aceptar o rechazar** una invitación (`POST /pets/invitations/{token}/accept|reject`), el
usuario debe:

1. Haber iniciado sesión con el **mismo correo** al que se envió la invitación (sin distinguir
   mayúsculas). Si no coincide: `403 INVITATION_EMAIL_MISMATCH`.
2. Tener el correo **verificado** en el User service. Si no: `403 EMAIL_NOT_VERIFIED`.

**Flujo recomendado para el frontend**: si el usuario abre el enlace del correo sin sesión activa,
guardar el `token` de la URL, pedir login (o registro) y, una vez autenticado, llamar al endpoint
con ese `token`. Si la respuesta es `INVITATION_EMAIL_MISMATCH`, mostrar que debe iniciar sesión con
el correo al que se envió la invitación (puede haberla abierto con otra cuenta). Si es
`EMAIL_NOT_VERIFIED`, llevarlo al flujo de verificación de correo del User service antes de
reintentar.

### 5.4 Tabla completa de códigos de error

Todas las respuestas de error devuelven `{"error": "mensaje", "code": "..."}`; `code` solo aparece
en las excepciones que lo definen (las demás siguen devolviendo solo `"error"`, sin cambios).

| `code` | HTTP | Cuándo | Mensaje sugerido (es) |
|---|---|---|---|
| `IDENTITY_MISMATCH` | 403 | El id/email legado enviado no coincide con el token (ni, cuando aplica, con el dueño real) | "No tienes permiso para actuar en nombre de otro usuario." |
| `PET_ACCESS_DENIED` | 403 | La mascota existe pero no tienes el rol necesario | "No tienes acceso a esta mascota." |
| `NOT_PET_OWNER` | 403 | La acción es exclusiva del dueño y no lo eres | "Solo el dueño de la mascota puede hacer esto." |
| `DOCUMENT_NOT_IN_PET` | 403 | El documento existe pero no pertenece a esa mascota | "Este documento no pertenece a esta mascota." |
| `CANNOT_SHARE_WITH_OWNER` | 400 | Intentas compartir, cambiar el rol o revocar el acceso del propio dueño | "No puedes compartir una mascota con su propio dueño." |
| `ACCESS_ALREADY_GRANTED` | 409 | Ese usuario ya tiene acceso a la mascota (al compartir o al aceptar una invitación) | "Este usuario ya tiene acceso a la mascota." |
| `INVITATION_EMAIL_MISMATCH` | 403 | El correo de tu sesión no es el de la invitación | "Debes iniciar sesión con el correo al que se envió la invitación." |
| `EMAIL_NOT_VERIFIED` | 403 | Tu correo no está verificado | "Verifica tu correo antes de continuar." |

(Los códigos que ya existían antes de PETS-02, como `404`/`401`/`400` de validación, no cambiaron —
ver §1.)

### 5.5 Qué cambió de 404 a 403 (o viceversa)

Antes de esta rama, algunos endpoints devolvían `404` cuando en realidad la mascota existía pero el
usuario no tenía acceso (o directamente no comprobaban nada). Ahora es consistente en todos lados:

- **Mascota no existe** → siempre `404`.
- **Mascota existe pero no tienes el rol necesario** → siempre `403` (antes, en varios endpoints de
  documentos y de lectura de mascota/edad, no había ninguna comprobación — cualquier usuario
  autenticado podía leer datos de cualquier mascota).

### 5.6 Tabla de migración por endpoint

| Antes | Ahora (ruta oficial) | Qué ya no hay que mandar | Alias deprecado |
|---|---|---|---|
| `POST /pets` con `ownerId` en el body | `POST /pets` sin `ownerId` | `ownerId` (si se manda, se ignora como identidad; ya no es obligatorio) | — |
| `GET /pets/user/{userId}` | `GET /pets/me` | — | `GET /pets/user/{userId}` sigue, solo responde si `{userId} == sub` |
| `PATCH /pets/{petId}` con `ownerId` | `PATCH /pets/{petId}` | `ownerId` (opcional, regla de identidad "una mascota") | query `ownerId` sigue aceptándose |
| `DELETE /pets/{petId}?ownerId=` | `DELETE /pets/{petId}` | `ownerId` | igual, opcional |
| `PATCH/DELETE /pets/{petId}/photo?ownerId=` | igual, sin `ownerId` | `ownerId` | igual, opcional |
| `POST/PATCH/DELETE /pets/{petId}/access...?ownerId=` | igual, sin `ownerId` | `ownerId` | igual, opcional |
| `GET /pets/shared?userId=` | `GET /pets/shared/me` | — | `GET /pets/shared?userId=` sigue, solo "lo mío" |
| `POST /pets/{petId}/invitations/resend`, `GET .../invitations/pending` (por mascota), `GET .../invitations/check` con `ownerId` | igual, sin `ownerId` | `ownerId` | igual, opcional |
| `POST /invitations/{token}/accept` con `{userId}` | `POST /pets/invitations/{token}/accept` sin body | `userId` | `POST /invitations/{token}/accept` sigue (body opcional) |
| `POST /invitations/{token}/reject` | `POST /pets/invitations/{token}/reject` | — | `POST /invitations/{token}/reject` sigue |
| `GET /pets/invitations/pending` con body `{email}` | `GET /pets/invitations/me` (sin body, sin header) | `{email}` / header `X-User-Email` | `GET /pets/invitations/pending` sigue, acepta body `{email}` **o** header `X-User-Email`, solo si coincide con el token |
| `POST /pets/{petId}/documents` con `uploadedBy` | igual, sin `uploadedBy` | `uploadedBy` (si se manda, solo se valida, nunca decide el autor real) | query `uploadedBy` sigue aceptándose |
| `GET .../download-url?requestedBy=`, `DELETE .../documents/{id}?requestedBy=` | igual, sin `requestedBy` | `requestedBy` | igual, opcional |

> **Nota**: ninguno de estos alias deprecados tiene fecha de eliminación todavía. Se quitarán en una
> rama futura, después de confirmar que el frontend ya no los usa. No hay apuro por migrar, pero
> conviene dejar de depender de ellos en código nuevo.

### 5.7 Checklist de migración para el frontend

- [ ] Dejar de mandar `ownerId` al crear una mascota (sigue aceptándose si se manda, pero ya no hace falta).
- [ ] Cambiar `GET /pets/user/{userId}` → `GET /pets/me`.
- [ ] Cambiar `GET /pets/shared?userId=` → `GET /pets/shared/me`.
- [ ] Cambiar `GET /pets/invitations/pending` (body `{email}` o header `X-User-Email`) → `GET /pets/invitations/me`.
- [ ] Cambiar las llamadas de aceptar/rechazar invitación a `/pets/invitations/{token}/accept|reject` (con el prefijo `/pets`).
- [ ] Manejar los nuevos `code` en las respuestas de error: al menos `IDENTITY_MISMATCH`, `INVITATION_EMAIL_MISMATCH`, `EMAIL_NOT_VERIFIED`, `CANNOT_SHARE_WITH_OWNER`, `ACCESS_ALREADY_GRANTED`.
- [ ] Si el flujo de invitaciones por enlace abre la app sin sesión, guardar el `token` y reintentar después del login (ver §5.3).
- [ ] Revisar pantallas que asumían que un EDITOR puede borrar cualquier documento de la mascota: ahora solo puede borrar los que él mismo subió, y pierde esa capacidad si lo bajan a VIEWER.
- [ ] No asumir que `ownerId`/`uploadedBy`/`requestedBy` siguen siendo obligatorios en ningún formulario — son opcionales en todos los endpoints donde existían.
