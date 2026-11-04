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

**Still mid-transition (PETS-02 will fix)**: identity is *also* still passed explicitly in bodies/query params — `ownerId` / `userId` / `uploadedBy` / `targetUserId` (e.g. `GrantAccessRequest.ownerId`, `UploadPetPhotoController`'s `@RequestParam UUID ownerId`). Services do their own checks in code, e.g. `if (!pet.getOwnerId().equals(request.getOwnerId())) throw new UnauthorizedPetAccessException();`. PETS-01 only added the requirement of a valid token; it did **not** start deriving identity from `sub`. Treat invitation code as in flux.

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
- Create (`POST /pets`, `PetService.add`), edit (`PATCH /pets/{petId}`, partial update — only non-null fields in `EditPetRequest` are applied, with validation: name not blank, height/weight > 0), delete (`DELETE /pets/{petId}`, owner-only — distinguishes "not found" vs "not yours" by re-querying), get by id, list by owner (`GET /pets/user/{userId}`), calculate age from `birthDate` (`GET /pets/{petId}/age`, throws `BirthDateNotRegisteredException` if unset).
- Photo: `PATCH /pets/{petId}/photo` (multipart, `ownerId` + `file` as query/part) uploads to Supabase bucket `pet-photos` at `pets/{petId}/profile.<ext>` (upsert) and updates `Pet.photoUrl`; `DELETE /pets/{petId}/photo` removes the file from storage and clears the field.

**Sharing / access control** — a pet has exactly one `ownerId`; other users get a `PetUserAccess` row (`petId`, `userId`, `accessRole: Rol`) either granted directly by the owner or via the invitation flow:
- `GrantAccessUseCase` (`POST /pets/{petId}/access`): owner-only, rejects granting to the owner themself (`CannotGrantAccessToOwnerException`) or duplicate grants (`AccessAlreadyGrantedException`).
- `UpdateRoleUseCase` (`PATCH /pets/{petId}/access/{targetUserId}`): owner-only, changes an existing access row's role, publishes `PET_ROLE_UPDATED`.
- `RevokeAccessUseCase` (`DELETE /pets/{petId}/access/{targetUserId}?ownerId=...`): owner-only.
- `HasPermissionUseCase` (`GET /pets/{petId}/access/{userId}`) and `IsOwnerUseCase` (`GET /pets/{petId}/owner/{userId}`) are read-only checks, presumably consumed by other services/the gateway for authorization decisions.
- `GetSharedProfilesByPetUseCase` (`GET /pets/{petId}/shared`, owner-only) lists everyone a pet is shared with; `GetSharedProfilesUseCase` (`GET /pets/shared?userId=...`) lists pets shared *with* a given user.

**Invitations** (`PetInvitation`: id, petId, userId (nullable until accepted), email, accessRole, status, token, expiresAt, createdAt) — email-based, token-based sharing flow:
1. `SendInvitationUseCase` (`POST /pets/{petId}/invitations`, owner-only): rejects if a PENDING invite already exists for that pet+email (`InvitationAlreadyExistsException`); creates a PENDING invitation with a random UUID token, expiring in 7 days; publishes `PET_INVITATION_SENT`.
2. `AcceptInvitationUseCase` (`POST /invitations/{token}/accept`, body: `userId`): validates the invitation is still PENDING and not expired (`InvitationAlreadyProcessedException` otherwise), rejects if the user already has access, marks the invitation ACCEPTED (stamping `userId`), creates a `PetUserAccess` row, publishes `PET_INVITATION_ACCEPTED`. **Note**: it does not currently verify that the accepting user's own email matches `invitation.email` — only the token is checked.
3. `RejectInvitationUseCase` (`POST /invitations/{token}/reject`): marks REJECTED, publishes `PET_INVITATION_REJECTED`.
4. `ResendInvitationUseCase` (`POST /pets/{petId}/invitations/resend`, owner-only): cancels *all* existing invitations for that pet+email (sets them CANCELLED) and creates a fresh PENDING one with a new token/expiry, publishes `PET_INVITATION_RESENT`.
5. Query helpers: `GetPendingInvitationsByPetUseCase` (`GET /pets/{petId}/invitations/pending`, owner-only), `GetPendingInvitationsByEmailUseCase` (`GET /pets/invitations/pending`, body `{ email }` — used so a user can see invitations sent to their email across all pets), `IsAlreadyInvitedUseCase` (`GET /pets/{petId}/invitations/check?email=...`), `IsExpiredUseCase` (`GET /pets/invitations/{token}/expired`).

**Pet documents** (`PetDocument`: id, petId, uploadedBy, title, description, fileUrl, mimeType, fileSizeBytes, documentType, uploadedAt, active) — stored in Supabase bucket `pet-documents` at `pets/{petId}/documents/{documentId}.<ext>`:
- `UploadPetDocumentUseCase` (`POST /pets/{petId}/documents`, multipart: `uploadedBy`, `title`, `description?`, `documentType`, `file`): validates file non-empty, ≤10MB, content-type in `{pdf, doc, docx, jpeg, png, webp}`; publishes `PET_DOCUMENT_UPLOADED`.
- `ListPetDocumentsUseCase` (`GET /pets/{petId}/documents`), `ListPetDocumentsByTypeUseCase` (`GET /pets/{petId}/documents/type/{documentType}`), `GetPetDocumentByIdUseCase` (`GET /pets/{petId}/documents/{documentId}`), `GetDownloadUrlUseCase` (`GET /pets/{petId}/documents/{documentId}/download-url` — currently just returns the stored public `fileUrl` as-is, no signed-URL generation), `UpdatePetDocumentMetadataUseCase` (`PATCH /pets/{petId}/documents/{documentId}/metadata` — title/description only, requires at least one non-blank field), `DeletePetDocumentUseCase` (`DELETE /pets/{petId}/documents/{documentId}` — deletes from storage then from DB). All of these check `document.getPetId().equals(petId)` and throw `DocumentNotBelongsToPetException` on mismatch.

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
