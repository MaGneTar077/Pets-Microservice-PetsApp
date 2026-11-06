# Cambios en pet-service (Pets) para MyAnimaLogVet

> Leer junto con `CLAUDE.md` del repo y `CONTRATOS_COMPARTIDOS.md`.
> Mantener las convenciones del repo: un `UseCase`/`Service`/`Controller`/`Request`/`Response` por operación, excepción de dominio + handler en `GlobalExceptionHandler`, evento vía `PublishPetEventUseCase`.
> **Recordatorio crítico del repo:** todo `eventType` nuevo debe agregarse a `TOPIC_BY_EVENT_TYPE` en `GooglePubSubPetEventAdapter` o se descarta en silencio. PETS-08 agrega un test para que eso no vuelva a pasar.

---

## PETS-01 — Validar el JWT *(requiere USER-01, USER-02)*

1. Agregar `spring-boot-starter-oauth2-resource-server` y `spring-security-test`.
2. Reescribir `SecurityConfig` igual que en Veterinary (`VETERINARY_SERVICE_PLAN.md §4.3`): JWKS, validación de `iss`/`aud`, `InternalApiKeyFilter` para `/internal/**`, `anyRequest().authenticated()`. Públicas: Swagger, actuator health/info.
3. Crear `AuthenticatedUserPort` + adaptador (mismo diseño que Veterinary §4.4).
4. Eliminar `jjwt` y `JWT_SECRET` al terminar.

## PETS-02 — Identidad desde el token en lugar del body *(hecho, rama `feature/pets-02-identidad-desde-token`)*

> Implementado íntegramente: `CurrentUserIdentity` + `PetAccessPolicy` en `application/security/`,
> matriz de roles OWNER/EDITOR/VIEWER, `code` en las respuestas de error, 403 vs 404 consistente.
> Documentado en `CLAUDE.md` y `docs/integracion/PETS_CAMBIOS_PARA_FRONTEND.md`. Los campos legados
> (`ownerId`, `userId`, `uploadedBy`, `requestedBy`, header `X-User-Email`) se mantienen como alias
> `@Deprecated` sin fecha de eliminación — no se borró ninguna ruta histórica.

Reemplazar por `sub` del token **todos** estos campos, y quitarlos de los DTO de request:

| Dónde | Campo que se elimina |
|---|---|
| `AddPetRequest`, `EditPetRequest`, delete | `ownerId` |
| `UploadPetPhotoController` | `@RequestParam UUID ownerId` |
| `GrantAccessRequest`, `UpdateRole`, `RevokeAccess` (`?ownerId=`) | `ownerId` |
| `SendInvitation`, `ResendInvitation`, `GetPendingInvitationsByPet` | `ownerId` |
| `AcceptInvitation` | `userId` del body |
| `GetPendingInvitationsByEmail` | `{ email }` del body → usar `email` del token (y cambiar a `GET` sin body) |
| `UploadPetDocument` | `uploadedBy` |
| `GET /pets/user/{userId}` | reemplazar por `GET /pets/me` (el dueño solo lista lo suyo) |
| `GET /pets/shared?userId=` | reemplazar por `GET /pets/shared/me` |

**Corregir de paso el hueco conocido:** `AcceptInvitationService` debe verificar que el `email` del token sea igual a `invitation.email` (case-insensitive) y que `email_verified = true`. Nueva excepción `InvitationEmailMismatchException` → 403.

## PETS-03 — Lectura de mascotas por personal de una clínica *(requiere VET-29)*

Cuando llega un token con `ctx = VETERINARY`:

1. Nuevo puerto `VeterinaryAccessPort` con `check(veterinaryId, petId, userId)` → adaptador `RestClient` a `GET /internal/veterinaries/{vet}/patients/{pet}/access?userId=` con caché de 60 s (Caffeine).
2. En `GetPetById`, `CalculateAge`, `ListPetDocuments`, `ListPetDocumentsByType`, `GetPetDocumentById`, `GetDownloadUrl`: si el token es de clínica, permitir si `hasAccess` y tiene `CLINICAL_READ` o `CLINICAL_READ_BASIC` (para documentos: `CLINICAL_READ`).
3. `UploadPetDocument` con token de clínica: requiere `DOCUMENT_UPLOAD`. `uploadedBy` = `sub`. Agregar columna `uploaded_by_veterinary_id UUID` a `pet_documents`.
4. Con token de clínica **no** se permite: editar mascota, borrar mascota, foto de perfil, compartir, invitaciones, borrar documentos que no subió esa clínica.
5. Lógica centralizada en un servicio `PetAccessPolicy` (no repetir `if` en cada servicio).

## PETS-04 — Endpoints internos de lectura *(requerido por VET-25)*

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/internal/pets/{petId}/owner/{userId}` | `{ isOwner: bool }` (reutiliza `IsOwnerUseCase`) |
| POST | `/internal/pets/batch` | body `{ ids: [] }` (máx. 100) → `[{ id, ownerId, name, species, breed, sex, birthDate, photoUrl, createdByVeterinaryId, claimStatus }]` |

Los endpoints públicos `HasPermission` e `IsOwner` (`GET /pets/{petId}/access/{userId}`, `/owner/{userId}`) se mueven a `/internal/**`: hoy cualquiera puede consultar relaciones de cualquier usuario.

## PETS-05 — Mascotas registradas por una clínica (dueño sin app) *(requerido por VET-28)*

Migración (el repo no tiene Flyway; si se agrega, usar `V2__clinic_pets.sql`):

```sql
ALTER TABLE pets ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE pets
  ADD COLUMN created_by_veterinary_id UUID,
  ADD COLUMN owner_contact_name VARCHAR(200),
  ADD COLUMN owner_contact_phone VARCHAR(30),
  ADD COLUMN owner_contact_email VARCHAR(200),
  ADD COLUMN owner_contact_document VARCHAR(30),
  ADD COLUMN claim_status VARCHAR(20) NOT NULL DEFAULT 'NONE';  -- NONE, UNCLAIMED, CLAIMED
ALTER TABLE pets ADD CONSTRAINT chk_owner_or_clinic
  CHECK (owner_id IS NOT NULL OR created_by_veterinary_id IS NOT NULL);
```

- Actualizar `Pet` (dominio), `PetEntity`, mapper y DTOs de respuesta.
- **Revisar todo el código que asume `ownerId != null`**: `pet.getOwnerId().equals(...)` lanza `NullPointerException` con mascotas sin dueño. Invertir a `request.getOwnerId().equals(pet.getOwnerId())` o usar `Objects.equals`.
- Endpoints internos:

| Método | Ruta | Uso |
|---|---|---|
| POST | `/internal/pets/clinic` | Crea mascota con `owner_id = null`, `claim_status = UNCLAIMED`, `created_by_veterinary_id` y contacto. Valida igual que `AddPet`. Publica `PET_ADDED` con `userId = null` (no notificar) |
| DELETE | `/internal/pets/clinic/{petId}` | Compensación: solo borra si `claim_status = UNCLAIMED` y no tiene documentos |
| PATCH | `/internal/pets/clinic/{petId}` | Editar datos y contacto mientras siga `UNCLAIMED` (lo llama Veterinary con permiso `PATIENT_REGISTER`) |

## PETS-06 — Reclamar una mascota *(requerido por VET-28)*

```sql
CREATE TABLE pet_claim_invitations (
  id UUID PRIMARY KEY,
  pet_id UUID NOT NULL REFERENCES pets(id),
  veterinary_id UUID NOT NULL,
  email VARCHAR(200) NOT NULL,
  token UUID NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL,          -- PENDING, ACCEPTED, CANCELLED, EXPIRED
  expires_at TIMESTAMPTZ NOT NULL,       -- 30 días
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

| Método | Ruta | Token | Caso de uso |
|---|---|---|---|
| POST | `/internal/pets/{petId}/claim-invitations` | interno | `CreateClaimInvitation`: cancela las `PENDING` anteriores de esa mascota, crea una nueva, publica `PET_CLAIM_INVITATION_SENT` con `userId = null` y `recipient` = contacto del dueño, payload `{ petName, veterinaryName, claimUrl, expiresAt }` |
| GET | `/pets/claims/{token}` | base | `PreviewClaim`: datos básicos de la mascota y nombre de la clínica |
| POST | `/pets/claims/{token}/accept` | base | `AcceptClaim` |
| GET | `/pets/claims/me` | base | `GetMyPendingClaims` (por email del token) |

Reglas de `AcceptClaim`:
- Invitación `PENDING`, no vencida, mascota `UNCLAIMED`.
- `email` del token igual al de la invitación y `email_verified = true`.
- En una transacción: `owner_id = sub`, `claim_status = CLAIMED`, limpiar `owner_contact_*`, invitación `ACCEPTED`.
- Publica `PET_CLAIMED` con `userId = sub`.
- El texto en la app debe decir que la clínica que la registró **conserva el acceso** (y que puede revocarlo desde la app).

## PETS-07 — Migrar al sobre de evento común

1. Reemplazar `PetEvent` por el sobre de `CONTRATOS_COMPARTIDOS.md §4.1` (`eventType`, `userId`, `recipient`, `payload`, `occurredAt`, `metadata`).
2. `userId` = destinatario: por ejemplo, en `PET_INVITATION_SENT` el destinatario es el invitado (`null` + `recipient.email`), no el dueño.
3. Incluir detalle en `payload` (email invitado, rol, nombre del documento, etc.), que hoy no viaja.
4. Coordinar con NOTIF-01: el consumidor debe aceptar el sobre nuevo antes del despliegue.

## PETS-08 — Topics nuevos y test de cobertura

1. Agregar a `TOPIC_BY_EVENT_TYPE`: `PET_CLAIM_INVITATION_SENT → pet-claim-invitation-sent`, `PET_CLAIMED → pet-claimed`, `PET_DOCUMENT_UPLOADED_BY_CLINIC → pet-document-uploaded`.
2. Convertir los `eventType` en un enum y agregar un test que falle si algún valor no tiene topic.
3. Crear los topics en GCP: `gcloud pubsub topics create pet-claim-invitation-sent pet-claimed`.

---

## Tests mínimos

- Cada endpoint que antes recibía `ownerId` ahora usa el token: otro usuario → 403.
- `AcceptInvitation` con email distinto → 403.
- Token de clínica con acceso → lee la mascota; sin acceso → 403; intenta editar → 403.
- Mascota de clínica sin dueño no rompe ningún servicio (NPE).
- Reclamo: email distinto → 403; vencido → error; aceptado → `ownerId` asignado y evento publicado.
- Test de cobertura de topics.
