# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`Pets` (Maven artifact `MyAnimalLog`) is the **Pets microservice** of the MyAnimalLog platform — a Spring Boot 3.5 / Java 17 service that owns pet profiles, pet documents/photos, and pet-sharing (invitations + role-based access) between users. It is one service among several microservices (see `services.user-service.url` in config, which points at a separate User service this app calls/interacts with).

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

There is no linter/formatter config in the repo (no Checkstyle/Spotless plugin) — just Maven build + JUnit tests.

### Local configuration

The app loads env vars from an optional `.env` file at the project root (`spring.config.import: optional:file:.env[.properties]`, via `spring-dotenv`). Required variables (see `src/main/resources/application.yaml`): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_KEY`, `GCP_PROJECT_ID`, optionally `PORT` (default `8081`) and `USER_SERVICE_URL` (default `http://localhost:8080`).

Swagger/OpenAPI UI is available at `/swagger-ui.html` (docs at `/api-docs`) and is always publicly permitted, along with actuator `health`/`info`.

## Architecture

The codebase follows **hexagonal / ports-and-adapters architecture**, split into three layers under `src/main/java/com/MyAnimaLog/Pets/`:

- **`domain/`** — pure business objects with no framework dependencies: `model/` (Pet, PetDocument, PetInvitation, PetUserAccess — plain Lombok `@Data`/`@Builder` classes), `enums/` (Rol: OWNER/EDITOR/VIEWER, InvitationStatus, Sex, DocumentType), and `exceptions/` (one exception class per business rule violation, e.g. `PetNotBelongsToOwnerException`, `CannotGrantAccessToOwnerException`, `InvitationAlreadyProcessedException`).
- **`application/`** — use cases, driven entirely by interfaces:
  - `ports/in/` — one `*UseCase` interface per operation (e.g. `AddPetUseCase`, `GrantAccessUseCase`, `SendInvitationUseCase`), each with a single `execute(request)` method.
  - `ports/out/` — outbound interfaces implemented by infrastructure: `PetRepositoryPort`, `PetDocumentRepositoryPort`, `PetInvitationRepositoryPort`, `PetUserAccessRepositoryPort`, `PetPhotoStoragePort`, `PetDocumentStoragePort`, `EventPublisherPort`.
  - `services/` — one `*Service` class per use case, implementing the matching `*UseCase` interface and depending only on `ports/out` interfaces (never on infrastructure classes directly).
  - `dto/` — request/response records/builders per operation (e.g. `SendInvitationRequest` / `SendInvitationResponse`), plus `PetEvent` for outbound pub/sub events.
- **`infrastructure/`** — Spring-specific adapters:
  - `controllers/` — one `@RestController` per use case (mirrors the `ports/in` naming 1:1), thin: builds the request DTO (often merging path variables into the body via `toBuilder()`) and delegates to the use case.
  - `adapters/` — implementations of `ports/out`: `PetRepositoryAdapter`, `PetInvitationRepositoryAdapter`, `PetUserAccessRepositoryAdapter`, `PetDocumentRepositoryAdapter` (JPA-backed), `SupabasePetPhotoStorageAdapter` / `SupabasePetDocumentStorageAdapter` (Supabase storage via `RestTemplate`), `GooglePubSubPetEventAdapter` (publishes `PetEvent`s to Google Cloud Pub/Sub topics).
  - `repositories/` — Spring Data JPA repositories (`JpaPetRepository`, etc.) used only by the adapters, never injected into services directly.
  - `entity/` + `mapper/` — JPA `@Entity` classes and mappers translating between entities and domain models.
  - `config/` — `SecurityConfig` (Spring Security filter chain), `SupabaseConfig` (Supabase URL/key + `RestTemplate` bean), `GlobalExceptionHandler` (`@RestControllerAdvice` mapping every domain exception to an HTTP status).

**Convention**: every feature/operation gets a matching set of files across all three layers with consistent naming — `<Verb><Noun>UseCase` (port), `<Verb><Noun>Service` (impl), `<Verb><Noun>Controller` (adapter), `<Verb><Noun>Request`/`<Verb><Noun>Response` (dto). When adding a new operation, follow this same pattern rather than folding logic into an existing class.

### Security note

`SecurityConfig` currently `permitAll()`s every request (CSRF disabled, no JWT filter wired in) even though `jjwt` and a `JWT_SECRET` are configured — identity (`ownerId`, `userId`, `email`, etc.) is currently passed explicitly in request bodies/DTOs rather than extracted from an authenticated principal. Recent commits (see git log) are actively tightening identity validation in the invitation flow (e.g. checking the invitation email matches the acting user) — keep this in mind when touching auth-adjacent code, since the security model is mid-transition.

### Domain model summary

- A **Pet** has one `ownerId`. Other users get access via **PetUserAccess** rows with a `Rol` (OWNER/EDITOR/VIEWER), created either directly (`GrantAccessUseCase`) or through the **PetInvitation** flow (send → accept/reject/resend, with `InvitationStatus` and expiring tokens).
- **PetDocument** records (typed via `DocumentType`: VACCINE, MEDICAL_RECORD, PRESCRIPTION, etc.) and pet photos are stored in Supabase storage; the DB only holds metadata/URLs.
- Business-rule violations throw dedicated domain exceptions caught centrally by `GlobalExceptionHandler` and translated to the appropriate HTTP status (404/403/400/409/422/413/415 as applicable) — always add a new exception + handler pair rather than reusing a generic one when introducing a new business rule.
- Most write operations (add/edit pet, upload photo/document, invitations, role updates) publish a `PetEvent` through `EventPublisherPort` → `GooglePubSubPetEventAdapter`, which maps `eventType` to a fixed Pub/Sub topic name (see `TOPIC_BY_EVENT_TYPE`). When adding a new event type, register its topic there or it will silently be dropped (only logged as an error).

## Testing conventions

Tests live under `src/test/java/...`, mirroring the main package structure, split into `application/services/*ServiceTest` (Mockito-based unit tests of business logic, `@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks` on the `ports/out` interfaces) and `infrastructure/controllers/*ControllerTest` + `infrastructure/adapters/*AdapterTest`. Assertions use AssertJ (`org.assertj.core.api.Assertions`).
