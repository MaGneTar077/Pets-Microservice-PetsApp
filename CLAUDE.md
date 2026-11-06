# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`Pets` (Maven artifact `MyAnimalLog`, Spring app name `pet-service`) is the **Pets microservice** of the MyAnimalLog platform — a Spring Boot 3.5 / Java 17 service that owns pet profiles, pet photos/documents, and pet-sharing between users (invitations + role-based access). It is one of several microservices; it references a separate **User service** (`services.user-service.url`, default `http://localhost:8080`) and publishes domain events to **Google Cloud Pub/Sub** for other services to consume.

## Common commands

This project uses the Maven wrapper — prefer `mvnw.cmd` (Windows) / `./mvnw` (bash) over a globally installed Maven.

```powershell
# Build (compile + run tests)
.\mvnw.cmd clean install

# Run the app locally (reads .env via spring-dotenv, see below)
.\mvnw.cmd spring-boot:run

# Run the full test suite
.\mvnw.cmd test

# Run a single test class
.\mvnw.cmd test -Dtest=GrantAccessServiceTest

# Run a single test method
.\mvnw.cmd test -Dtest=GrantAccessServiceTest#grantsAccessWhenOwnerAndNotAlreadyGranted

# Package the runnable jar
.\mvnw.cmd package
```

There is no linter/formatter plugin configured (no Checkstyle/Spotless) — just Maven build + JUnit/Mockito/AssertJ tests.

### Local configuration

The app loads env vars from an optional `.env` file at the project root (`spring.config.import: optional:file:.env[.properties]`, via `spring-dotenv`). Required variables (see `src/main/resources/application.yaml`):

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection (Hikari, isolation level READ COMMITTED, `ddl-auto: none` — schema is managed outside the app, no Flyway/Liquibase in the repo) |
| `JWT_JWKS_URI` | JWKS of the User service (`{USER_SERVICE_URL}/.well-known/jwks.json`). **The app refuses to start without it** |
| `JWT_ISSUER` | Expected `iss`, default `myanimalog-user-service` |
| `JWT_AUDIENCE` | Expected `aud`, default `myanimalog-api` |
| `INTERNAL_API_KEY` | Shared secret for `/internal/**` (`X-Internal-Api-Key`). **The app refuses to start if missing or blank** |
| `SUPABASE_URL`, `SUPABASE_KEY` | Supabase Storage REST API, used for pet photos/documents |
| `GCP_PROJECT_ID` | Google Cloud project for Pub/Sub event publishing |
| `PORT` (default `8081`) | HTTP port |
| `USER_SERVICE_URL` (default `http://localhost:8080`) | Address of the sibling User microservice (not currently called from code — reserved/used by other parts of the platform) |

Swagger/OpenAPI UI is at `/swagger-ui.html` (docs at `/api-docs`), always publicly permitted, along with actuator `health`/`info` (`show-details: never`).

## Architecture

Hexagonal / ports-and-adapters, under `src/main/java/com/MyAnimaLog/Pets/`:

- **`domain/`** — framework-free business types:
  - `model/`: `Pet`, `PetDocument`, `PetInvitation`, `PetUserAccess` (Lombok `@Data`/`@Builder(toBuilder = true)` POJOs).
  - `enums/`: `Rol` (OWNER, EDITOR, VIEWER), `InvitationStatus` (PENDING, ACCEPTED, REJECTED, CANCELLED), `Sex`, `DocumentType` (VACCINE, MEDICAL_RECORD, PRESCRIPTION, LAB_RESULT, SURGERY_REPORT, DEWORMING, ALLERGY_REPORT, OTHER).
  - `exceptions/`: one class per business rule violation (e.g. `PetNotBelongsToOwnerException`, `CannotGrantAccessToOwnerException`, `InvitationAlreadyProcessedException`), all caught centrally.
- **`application/`**:
  - `ports/in/` — one `*UseCase` interface per operation.
  - `ports/out/` — outbound interfaces: `PetRepositoryPort`, `PetDocumentRepositoryPort`, `PetInvitationRepositoryPort`, `PetUserAccessRepositoryPort`, `PetPhotoStoragePort`, `PetDocumentStoragePort`, `EventPublisherPort`, `AuthenticatedUserPort`.
  - `services/` — one `*Service` per use case, depending only on `ports/out`. **Exceptions to the 1:1 naming**: pet creation is implemented by `PetService` (implements `AddPetUseCase`, also exposes `add(AddPetRequest)` called directly by `AddPetController`), and event publishing is centralized in `PublishPetEventService` (implements `PublishPetEventUseCase`, delegates to `EventPublisherPort`) which every mutating service injects to emit a `PetEvent`.
  - `dto/` — request/response classes per operation, plus `PetEvent` (`petId`, `ownerId`, `petName`, `eventType`, `occurredAt`).
- **`infrastructure/`**:
  - `controllers/` — thin `@RestController`s; typically merge path variables into the body DTO via `toBuilder()` before calling the use case.
  - `adapters/` — `PetRepositoryAdapter`, `PetInvitationRepositoryAdapter`, `PetUserAccessRepositoryAdapter`, `PetDocumentRepositoryAdapter` (JPA-backed via mappers), `SupabasePetPhotoStorageAdapter` / `SupabasePetDocumentStorageAdapter` (raw `RestTemplate` calls to the Supabase Storage REST API), `GooglePubSubPetEventAdapter` (publishes `PetEvent` JSON to a fixed Pub/Sub topic per event type).
  - `repositories/` — Spring Data JPA repos (`JpaPetRepository`, etc.), only used by adapters.
  - `entity/` + `mapper/` — JPA `@Entity` classes (tables `pets`, `pet_documents`, `pet_invitations`, `pet_user_access`) and domain↔entity mappers.
  - `config/` — `SecurityConfig`, `SupabaseConfig` (URL/key + `RestTemplate` bean), `GlobalExceptionHandler` (`@RestControllerAdvice`).
  - `security/` — `InternalApiKeyFilter`, `JwtClaimsConverter`, `JwtTokenValidator`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `SecurityErrorResponseWriter` (see Security below). `SecurityConfig` stays in `config/` because ~30 controller tests exclude it by that type.

**Convention when adding a new operation**: create matching `<Verb><Noun>UseCase` / `<Verb><Noun>Service` / `<Verb><Noun>Controller` / `<Verb><Noun>Request`+`Response`, wire a new domain exception if it's a new business rule, and add the matching `@ExceptionHandler` in `GlobalExceptionHandler`.

### Security (PETS-01, done)

Two independent authentication mechanisms, wired in `infrastructure/config/SecurityConfig.java` with the supporting classes in `infrastructure/security/`:

1. **User JWT (RS256 via JWKS)** for every route except the public ones. `JwtDecoder` is built from `app.security.jwks-uri` and validates signature plus `iss`/`aud` through `JwtTokenValidator`. `JwtClaimsConverter` deliberately grants **no** authorities, so no token claim can ever satisfy `INTERNAL_SERVICE`. Vet-role authorities arrive in PETS-03.
2. **`X-Internal-Api-Key`** for `/internal/**`, handled by `InternalApiKeyFilter` (constant-time `MessageDigest.isEqual`, configured key is `trim()`ed). `/internal/**` does **not** accept user JWTs, and a user JWT cannot reach it. The filter is instantiated directly in `SecurityConfig` rather than being a `@Component`, so Spring Boot does not also register it as a servlet filter outside the security chain.

Public routes are only Swagger (`/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**`) and actuator `health`/`info`; `anyRequest().authenticated()`.

**Fail-fast**: a blank/missing `INTERNAL_API_KEY` or `JWT_JWKS_URI` throws `IllegalStateException` while building the chain, so the app does not start. A blank key would otherwise match an empty header and open `/internal/**`.

401/403 are rendered as JSON `{"error": "..."}` by `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` (never Spring's default page), matching `GlobalExceptionHandler`'s format.

`AuthenticatedUserPort` (`application/ports/out`) exposes `sub`, `email` and `email_verified` from the token through `SpringAuthenticatedUserAdapter`; `AuthenticatedUser` is the DTO. `jjwt`/`JWT_SECRET` were removed (nothing used them).

### Identity & authorization (PETS-02, done)

Identity for authorization decisions now always comes from the validated JWT (`sub`/`email`/`email_verified`), never trusted from the request body/query/path. Two classes in `application/security/` centralize this:

- **`CurrentUserIdentity`** — wraps `AuthenticatedUserPort`. `resolveUserId(fromRequest)` / `resolveEmail(fromRequest)` implement the **strict "mine"-only compatibility rule**: `null`/absent → use the token; present and equal to the token (case-insensitive for email) → accept; present and different → `IdentityMismatchException` (403 `IDENTITY_MISMATCH`). Used by endpoints that return "my own" things: `/pets/me`, `/pets/shared/me`, `/pets/invitations/me`, and their deprecated `?userId=`/`X-User-Email` aliases.
- **`PetAccessPolicy`** — resolves the caller's `Rol` for a given pet (`OWNER` if `pet.ownerId == sub`, else whatever `pet_user_access` says, else no access) and exposes:
  - `requireOwner(petId[, legacyOwnerId])`, `requireWrite(petId[, legacyOwnerId])` (OWNER or EDITOR), `requireRead(petId[, legacyOwnerId])` (OWNER, EDITOR or VIEWER) — all throw `PetNotFoundException` (404) if the pet doesn't exist, `PetNotBelongsToOwnerException`/`UnauthorizedPetAccessException` (403) if it exists but the caller lacks the role.
  - `requireOwnerOrUploader(petId, document.uploadedBy)` — OWNER, or the original uploader **only if their current role is still EDITOR** (a downgraded EDITOR→VIEWER loses edit/delete rights over documents they uploaded earlier). VIEWER never, regardless of `uploadedBy`.
  - The optional `legacyOwnerId` parameter implements the **pet-aware compatibility rule**, for single-pet endpoints where the old frontend still sends the real owner's id: absent → skip; equal to the token's `sub` **or** equal to the pet's actual `owner_id` → accept (this does not itself grant any extra permission — the role check still decides); anything else → `IdentityMismatchException` (403). Used by `PATCH/DELETE /pets/{petId}`, `.../photo`, sharing, invitations, documents — anywhere identity denotes "which pet", as opposed to "my own stuff".

**Role matrix** (`Rol`): **OWNER** — everything. **EDITOR** — read pet/age/docs/download-url, edit pet data + photo, upload documents, edit-metadata/delete only on documents they themselves uploaded. **VIEWER** — read-only (pet/age/docs/download-url), never edits or deletes anything, including their own uploads (VIEWERs can't upload in the first place).

**Access semantics**: 403 when the pet exists but the caller lacks the required role; 404 when the pet doesn't exist. Consistent across every endpoint.

**Error format**: `GlobalExceptionHandler` additionally returns `"code"` (e.g. `PET_ACCESS_DENIED`, `NOT_PET_OWNER`, `IDENTITY_MISMATCH`, `CANNOT_SHARE_WITH_OWNER`, `ACCESS_ALREADY_GRANTED`, `INVITATION_EMAIL_MISMATCH`, `EMAIL_NOT_VERIFIED`, `DOCUMENT_NOT_IN_PET`) alongside `"error"` for authorization-related exceptions — additive, only present when the exception defines one. See `docs/integracion/PETS_CAMBIOS_PARA_FRONTEND.md` for the full table.

**Deprecated aliases still live** (kept working for the old frontend, not yet scheduled for removal): `GET /pets/user/{userId}` (→ `/pets/me`), `GET /pets/shared?userId=` (→ `/pets/shared/me`), `GET /pets/invitations/pending` with body `{email}` or header `X-User-Email` (→ `/pets/invitations/me`), `POST /invitations/{token}/accept` and `POST /invitations/{token}/reject` (→ `/pets/invitations/{token}/accept|reject`), `GET /pets/{petId}/owner/{userId}` and `GET /pets/{petId}/access/{userId}` (PETS-04, → `/internal/pets/...`).

### Internal endpoints (PETS-04, done)

Under `/internal/pets`, only reachable with `X-Internal-Api-Key`, never exposed through the gateway. Consumed by Medical and Veterinary.

| Method | Route | Response |
|---|---|---|
| GET | `/internal/pets/{petId}/owner/{userId}` | `200 {"isOwner": bool}` — a nonexistent pet returns `false`, **never 404** (Medical treats any non-2xx as the service being down) |
| POST | `/internal/pets/batch` | body `{"ids": [...]}`, max 100 (more → 400, empty → `200 []`); returns an **array**, silently omitting ids that don't exist |

- `InternalIsOwnerResponse` is a `record` on purpose: Lombok would serialize the field as `owner`, and Medical's contract needs exactly `isOwner`. The public alias still returns `owner`.
- `IsOwnerService` uses a single `existsByIdAndOwnerId` query (primary-key index, no exception flow) to stay well inside Medical's 3 s budget. Batch uses one `findAllById`, no N+1.
- **PETS-05 columns don't exist yet** (`claim_status`, `created_by_veterinary_id`, `owner_contact_*`), so the batch response hardcodes `claimStatus: "NONE"`, `createdByVeterinaryId: null`, `ownerContact: null`. When PETS-05 lands: populate them, send `ownerContact` only when `claimStatus = UNCLAIMED`, and make `IsOwnerService` return `false` for `UNCLAIMED` pets.
- The former public `IsOwner` / `HasPermission` routes (`GET /pets/{petId}/owner/{userId}`, `GET /pets/{petId}/access/{userId}`) remain as `@Deprecated` aliases: they now require a token **and** only answer when `{userId}` equals the token's `sub` (otherwise 403), so they no longer leak other users' relationships. See `docs/integracion/PETS_CAMBIOS_PARA_FRONTEND.md`.

### Domain model & business flows

**Pets** (`Pet`: id, ownerId, name, species, breed, sex, birthDate, height, weight, photoUrl, timestamps):
- Create (`POST /pets`, `PetService.add`, owner = token `sub`), edit (`PATCH /pets/{petId}`, `petAccessPolicy.requireWrite` — owner or EDITOR; partial update — only non-null fields in `EditPetRequest` are applied, with validation: name not blank, height/weight > 0), delete (`DELETE /pets/{petId}`, `requireOwner`), get by id (`requireRead` — owner, EDITOR or VIEWER), list my own pets (`GET /pets/me`, official; `GET /pets/user/{userId}` deprecated alias), calculate age from `birthDate` (`GET /pets/{petId}/age`, `requireRead`, throws `BirthDateNotRegisteredException` if unset).
- Photo: `PATCH /pets/{petId}/photo` (multipart, legacy `ownerId` + `file` as query/part, `requireWrite`) uploads to Supabase bucket `pet-photos` at `pets/{petId}/profile.<ext>` (upsert) and updates `Pet.photoUrl`; `DELETE /pets/{petId}/photo` (`requireWrite`) removes the file from storage and clears the field.

**Sharing / access control** — a pet has exactly one `ownerId`; other users get a `PetUserAccess` row (`petId`, `userId`, `accessRole: Rol`) either granted directly by the owner or via the invitation flow. All owner-only operations go through `petAccessPolicy.requireOwner(petId, legacyOwnerId)`:
- `GrantAccessUseCase` (`POST /pets/{petId}/access`): owner-only, rejects granting to the owner themself (`CannotGrantAccessToOwnerException`, 400 `CANNOT_SHARE_WITH_OWNER`) or duplicate grants (`AccessAlreadyGrantedException`, 409 `ACCESS_ALREADY_GRANTED`).
- `UpdateRoleUseCase` (`PATCH /pets/{petId}/access/{targetUserId}`): owner-only, same owner-protection as above, changes an existing access row's role, publishes `PET_ROLE_UPDATED`.
- `RevokeAccessUseCase` (`DELETE /pets/{petId}/access/{targetUserId}?ownerId=...`): owner-only, same owner-protection.
- `HasPermissionUseCase` (`GET /pets/{petId}/access/{userId}`) and `IsOwnerUseCase` (`GET /pets/{petId}/owner/{userId}`) are deprecated read-only checks (see PETS-04 below) — superseded by `/internal/pets/...` for service-to-service use.
- `GetSharedProfilesByPetUseCase` (`GET /pets/{petId}/shared`, owner-only) lists everyone a pet is shared with; `GetSharedProfilesUseCase` lists pets shared *with* a user — official `GET /pets/shared/me` (token), deprecated alias `GET /pets/shared?userId=` (`currentUserIdentity.resolveUserId`).

**Invitations** (`PetInvitation`: id, petId, userId (nullable until accepted), email, accessRole, status, token, expiresAt, createdAt) — email-based, token-based sharing flow:
1. `SendInvitationUseCase` (`POST /pets/{petId}/invitations`, owner-only): rejects if a PENDING invite already exists for that pet+email (`InvitationAlreadyExistsException`); creates a PENDING invitation with a random UUID token, expiring in 7 days; publishes `PET_INVITATION_SENT`.
2. `AcceptInvitationUseCase` (official `POST /pets/invitations/{token}/accept`, deprecated alias `POST /invitations/{token}/accept`; body `userId` is legacy, identity = `currentUserIdentity.resolveUserId`): validates the invitation is still PENDING and not expired (`InvitationAlreadyProcessedException` otherwise), rejects if the user already has access, **and requires the token's own `email` to match `invitation.email` case-insensitively (`InvitationEmailMismatchException`, 403 `INVITATION_EMAIL_MISMATCH`) with `email_verified = true` (`EmailNotVerifiedException`, 403 `EMAIL_NOT_VERIFIED`)** — this closes the PETS-01-era gap where any authenticated user holding the link could accept someone else's invitation. On success, marks the invitation ACCEPTED (stamping `userId` = token `sub`), creates a `PetUserAccess` row, publishes `PET_INVITATION_ACCEPTED`.
3. `RejectInvitationUseCase` (official `POST /pets/invitations/{token}/reject`, deprecated alias `POST /invitations/{token}/reject`): same email-match + verified checks as accept (no `userId` involved), marks REJECTED, publishes `PET_INVITATION_REJECTED`.
4. `ResendInvitationUseCase` (`POST /pets/{petId}/invitations/resend`, owner-only): cancels *all* existing invitations for that pet+email (sets them CANCELLED) and creates a fresh PENDING one with a new token/expiry, publishes `PET_INVITATION_RESENT`.
5. Query helpers: `GetPendingInvitationsByPetUseCase` (`GET /pets/{petId}/invitations/pending`, owner-only), `GetPendingInvitationsByEmailUseCase` — official `GET /pets/invitations/me` (email = token); deprecated alias `GET /pets/invitations/pending` accepts header `X-User-Email` or body `{ email }`, only honored if it matches the token's email (`currentUserIdentity.resolveEmail`), otherwise 403 `IDENTITY_MISMATCH` — `IsAlreadyInvitedUseCase` (`GET /pets/{petId}/invitations/check?email=...`, owner-only, legacy `ownerId` query optional), `IsExpiredUseCase` (`GET /pets/invitations/{token}/expired`).

**Pet documents** (`PetDocument`: id, petId, uploadedBy, title, description, fileUrl, mimeType, fileSizeBytes, documentType, uploadedAt, active) — stored in Supabase bucket `pet-documents` at `pets/{petId}/documents/{documentId}.<ext>`:
- `UploadPetDocumentUseCase` (`POST /pets/{petId}/documents`, multipart: legacy `uploadedBy`, `title`, `description?`, `documentType`, `file`): `requireWrite` (owner or EDITOR); `uploadedBy` is always the token's `sub` (the query param only follows the identity rule for validation). Validates file non-empty, ≤10MB, content-type in `{pdf, doc, docx, jpeg, png, webp}`; publishes `PET_DOCUMENT_UPLOADED`.
- `ListPetDocumentsUseCase`, `ListPetDocumentsByTypeUseCase`, `GetPetDocumentByIdUseCase`, `GetDownloadUrlUseCase` (`GET /pets/{petId}/documents/{documentId}/download-url` — currently just returns the stored public `fileUrl` as-is, no signed-URL generation; legacy `requestedBy` query is optional and follows the identity rule, the URL is only generated after `requireRead` passes): all `requireRead` (owner, EDITOR or VIEWER).
- `UpdatePetDocumentMetadataUseCase` (`PATCH /pets/{petId}/documents/{documentId}/metadata` — title/description only, requires at least one non-blank field) and `DeletePetDocumentUseCase` (`DELETE /pets/{petId}/documents/{documentId}`, legacy `requestedBy` optional): both `requireOwnerOrUploader` — owner, or EDITOR only if they themselves uploaded it; VIEWER never. All document endpoints check `document.getPetId().equals(petId)` and throw `DocumentNotBelongsToPetException` (403 `DOCUMENT_NOT_IN_PET`) on mismatch.

### Event publishing (Google Cloud Pub/Sub)

Every mutating use case injects `PublishPetEventUseCase` (→ `PublishPetEventService` → `EventPublisherPort` → `GooglePubSubPetEventAdapter`) and publishes a `PetEvent` after persisting. `GooglePubSubPetEventAdapter` maps `eventType` to a **hardcoded topic name** via `TOPIC_BY_EVENT_TYPE`:

```
PET_ADDED → pet-added                       PET_INVITATION_SENT → pet-invitation-sent
PET_EDITED → pet-edited                     PET_INVITATION_ACCEPTED → pet-invitation-accepted
PET_PHOTO_UPLOADED → pet-photo-uploaded      PET_INVITATION_REJECTED → pet-invitation-rejected
PET_DOCUMENT_UPLOADED → pet-document-uploaded  PET_INVITATION_RESENT → pet-invitation-resent
PET_ROLE_UPDATED → pet-role-updated          PET_SHARED → pet-shared (not currently emitted anywhere)
```

If `eventType` has no entry in that map, the event is **silently dropped** (only logged as an error) — when adding a new event type, you must add its topic here or publishing is a no-op. The event payload is just `{petId, ownerId, petName, eventType, occurredAt}` — no per-event-type detail (e.g. invitation email, document id) is included.

### Error handling

`GlobalExceptionHandler` maps every domain exception to a specific HTTP status (404 not-found, 403 forbidden/unauthorized, 401 unauthenticated, 400 bad request, 409 conflict, 422 unprocessable, 413 payload too large, 415 unsupported media type), plus `MethodArgumentNotValidException` → 400 with field errors, and a catch-all `Exception` → 500 that logs the detail but only returns `"Error interno del servidor"` (never SQL or stack traces).

Client-side mistakes must not surface as 500s: `NoResourceFoundException`/`NoHandlerFoundException` → **404** (an unmapped route used to return 500, which made Medical read it as an outage), `HttpRequestMethodNotSupportedException` → 405, `HttpMessageNotReadableException` → 400, `MethodArgumentTypeMismatchException` (malformed UUID in the path) → 400. Note that because `anyRequest().authenticated()` runs first, an *unauthenticated* request to an unknown route returns 401, not 404. When introducing a new business rule, add a dedicated exception class under `domain/exceptions/` and a matching handler here rather than reusing an existing one.

## Testing conventions

Tests mirror the main package layout under `src/test/java/...`:
- `application/services/*ServiceTest` — Mockito unit tests of business logic (`@ExtendWith(MockitoExtension.class)`, `@Mock` on `ports/out` interfaces, `@InjectMocks` on the service).
- `infrastructure/controllers/*ControllerTest` and `infrastructure/adapters/*AdapterTest` — adapter/web-layer tests.
- Assertions use AssertJ (`org.assertj.core.api.Assertions`).
