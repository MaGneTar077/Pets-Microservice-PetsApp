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
| `JWT_SECRET` | Configured for `jjwt`, but **not currently wired into any filter** (see Security below) |
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
  - `ports/out/` — outbound interfaces: `PetRepositoryPort`, `PetDocumentRepositoryPort`, `PetInvitationRepositoryPort`, `PetUserAccessRepositoryPort`, `PetPhotoStoragePort`, `PetDocumentStoragePort`, `EventPublisherPort`.
  - `services/` — one `*Service` per use case, depending only on `ports/out`. **Exceptions to the 1:1 naming**: pet creation is implemented by `PetService` (implements `AddPetUseCase`, also exposes `add(AddPetRequest)` called directly by `AddPetController`), and event publishing is centralized in `PublishPetEventService` (implements `PublishPetEventUseCase`, delegates to `EventPublisherPort`) which every mutating service injects to emit a `PetEvent`.
  - `dto/` — request/response classes per operation, plus `PetEvent` (`petId`, `ownerId`, `petName`, `eventType`, `occurredAt`).
- **`infrastructure/`**:
  - `controllers/` — thin `@RestController`s; typically merge path variables into the body DTO via `toBuilder()` before calling the use case.
  - `adapters/` — `PetRepositoryAdapter`, `PetInvitationRepositoryAdapter`, `PetUserAccessRepositoryAdapter`, `PetDocumentRepositoryAdapter` (JPA-backed via mappers), `SupabasePetPhotoStorageAdapter` / `SupabasePetDocumentStorageAdapter` (raw `RestTemplate` calls to the Supabase Storage REST API), `GooglePubSubPetEventAdapter` (publishes `PetEvent` JSON to a fixed Pub/Sub topic per event type).
  - `repositories/` — Spring Data JPA repos (`JpaPetRepository`, etc.), only used by adapters.
  - `entity/` + `mapper/` — JPA `@Entity` classes (tables `pets`, `pet_documents`, `pet_invitations`, `pet_user_access`) and domain↔entity mappers.
  - `config/` — `SecurityConfig`, `SupabaseConfig` (URL/key + `RestTemplate` bean), `GlobalExceptionHandler` (`@RestControllerAdvice`).

**Convention when adding a new operation**: create matching `<Verb><Noun>UseCase` / `<Verb><Noun>Service` / `<Verb><Noun>Controller` / `<Verb><Noun>Request`+`Response`, wire a new domain exception if it's a new business rule, and add the matching `@ExceptionHandler` in `GlobalExceptionHandler`.

### Security status (important, mid-transition)

`SecurityConfig` currently does `csrf().disable()` + `anyRequest().permitAll()` — **no authentication/authorization is enforced by Spring Security**, despite `jjwt` and `JWT_SECRET` being configured (likely reserved for future use or shared from a template). Callers (an API gateway or the frontend/BFF) are trusted to pass identity explicitly:
- `ownerId` / `userId` / `uploadedBy` / `targetUserId` are passed as request body fields or query params (e.g. `GrantAccessRequest.ownerId`, `UploadPetPhotoController`'s `@RequestParam UUID ownerId`), not derived from a token.
- Services then do their own authorization checks in code, e.g. `if (!pet.getOwnerId().equals(request.getOwnerId())) throw new UnauthorizedPetAccessException();`.
- Recent commits (`fix(invitations): corrige rutas y valida la identidad...`, `fix: add user email in pending invitation by email`) are actively reworking the invitation endpoints' routes and identity handling — treat this area as in flux when touching invitation code.

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

`GlobalExceptionHandler` maps every domain exception to a specific HTTP status (404 not-found, 403 forbidden/unauthorized, 400 bad request, 409 conflict, 422 unprocessable, 413 payload too large, 415 unsupported media type), plus `MethodArgumentNotValidException` → 400 with field errors, and a catch-all `Exception` → 500. When introducing a new business rule, add a dedicated exception class under `domain/exceptions/` and a matching handler here rather than reusing an existing one.

## Testing conventions

Tests mirror the main package layout under `src/test/java/...`:
- `application/services/*ServiceTest` — Mockito unit tests of business logic (`@ExtendWith(MockitoExtension.class)`, `@Mock` on `ports/out` interfaces, `@InjectMocks` on the service).
- `infrastructure/controllers/*ControllerTest` and `infrastructure/adapters/*AdapterTest` — adapter/web-layer tests.
- Assertions use AssertJ (`org.assertj.core.api.Assertions`).
