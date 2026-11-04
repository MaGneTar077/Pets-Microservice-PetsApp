# Pets: cambios de autenticación para el frontend

> Rama `feature/pets-jwks-y-endpoints-internos` (PETS-01 y PETS-04). Pendiente de despliegue.

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
| POST | `/invitations/{token}/accept` |
| POST | `/invitations/{token}/reject` |

> Ojo con `GET /pets/invitations/{token}/expired` y los dos `POST /invitations/{token}/...`: si la
> app abre el enlace del correo **antes** de iniciar sesión, ahora recibirá `401`. El flujo debe
> pedir login primero y luego llamar con el token del usuario.

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

## 5. Lo que viene (PETS-02, rama siguiente)

Hoy la identidad todavía viaja en el cuerpo o en query params (`ownerId`, `userId`, `uploadedBy`,
`targetUserId`). **En esta rama no cambió nada de eso**: solo se añadió la exigencia del token.

En PETS-02 esos campos se eliminan de los DTO y se toman del `sub` del token. Eso implicará, entre
otros: `POST /pets` sin `ownerId`, `GET /pets/user/{userId}` → `GET /pets/me`,
`GET /pets/shared?userId=` → `GET /pets/shared/me`. Se documentará aparte antes de desplegarlo.
