# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Tu es un Staff Software Engineer avec une forte expérience Product.

Ne te contente jamais d'exécuter mes demandes.

Pour chaque fonctionnalité :
- Challenge le besoin.
- Propose au moins 3 améliorations produit.
- Identifie les cas limites.
- Détecte les incohérences métier.
- Suggère des simplifications UX.
- Évalue les impacts sur la sécurité, les performances et la maintenabilité.
- Propose des métriques de succès (KPIs).
- Si une fonctionnalité manque, dis-le explicitement.
- Si une meilleure solution existe, explique pourquoi et recommande-la.

Considère-toi comme un membre de l'équipe produit, pas seulement comme un développeur.
## Project overview

Ummati ("Plateforme de Bénévolat et Gestion ONG") is a volunteering/NGO management platform. It's a monorepo with two independent projects:

- `ummati/` — Spring Boot 4 (Java 21) REST API backend
- `ummati-front/` — Angular 21 frontend (standalone components, Angular Material, SSR-capable)

The backend and frontend are developed and run independently; the frontend proxies `/api/*` to the backend during local dev.

Product/ticket context (sprints, feature scope, RM-xx business rules) lives in `ROADMAP.md` at the repo root — check it for the "why" behind a feature if the intent isn't obvious from code. **Test-driven development is a stated project requirement** (see `ROADMAP.md` top note): new backend features are expected to ship with unit tests (services) and integration tests (controllers).

## Local environment setup

Full step-by-step local setup (Docker Postgres on port **5433**, MailHog for dev email, ports) is documented in `LANCEMENT.md` — read it before trying to run the stack for the first time. Production deployment (containers, secrets, rotation, CI) is documented separately in `DEPLOIEMENT.md`. Key points:

- Dev Postgres runs in Docker on port **5433** (not 5432, which conflicts with a local Windows install).
- Flyway is **enabled** and applies `ummati/src/main/resources/db/migration/V*.sql` automatically at startup, in every profile. The `dev` profile sets `baseline-version: 17` to adopt databases that were migrated by hand before Flyway was turned on.
- Migrations must work **on an empty database**, not just on an already-migrated one — `V8` silently broke fresh installs for months because it added a constraint that `V3` had already created. The CI job "Déploiement depuis zéro" now guards this.
- MailHog intercepts all dev emails at http://localhost:8025 — no real email is ever sent locally.
- Frontend dev proxy (`ummati-front/proxy.conf.json`) forwards `/api/*` and `/uploads/*` → `http://localhost:8080`.
- Spring Boot 4 ships auto-configurations as separate modules: a library on the classpath (e.g. `flyway-core`) does nothing without its `spring-boot-*` companion. Check the dependency tree when an integration seems inert.

## Common commands

### Backend (`ummati/`)

```powershell
cd ummati
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"   # run locally (port 8080)
.\mvnw.cmd test                                                # run all tests
.\mvnw.cmd test "-Dtest=EventServiceTest"                      # run a single test class
.\mvnw.cmd test "-Dtest=EventServiceTest#someMethodName"       # run a single test method
.\mvnw.cmd compile                                              # compile only
```

Unit tests (`service/*Test.java`) mock dependencies directly. Integration tests (`controller/*IntegrationTest.java`) use Testcontainers (Postgres) or the H2 profile (`application-h2test.yaml`, `src/test/resources/db/h2migration/`) plus RestAssured/MockMvc.

Swagger UI is available at http://localhost:8080/swagger-ui.html when the backend is running; OpenAPI JSON at `/api-docs`.

### Frontend (`ummati-front/`)

```powershell
cd ummati-front
npm install
npm start                 # ng serve, http://localhost:4200
npm run build             # production build to dist/
npm test                  # ng test (Vitest runner)
ng generate component pages/some-feature/some-component   # scaffold a standalone component
```

### End-to-end smoke test

`test-e2e.ps1` (repo root) is a PowerShell script that exercises the full backend API surface (register → verify → login → onboarding → orgs → events → signups → memberships → notifications → admin) against a **running local backend** (`http://localhost:8080`) with a real Postgres/MailHog stack. It self-throttles around the `/auth/**` rate limiter (20 req/min). Run it after backend changes that touch auth, orgs, events, or membership flows:

```powershell
.\test-e2e.ps1
```

## Backend architecture (`ummati/src/main/java/orga/takwa/ummati/`)

Layered, package-by-type structure:

- `entity/` — JPA entities (`User`, `Organization`, `Membership`, `Event`, `EventSignup`, `EventFeedback`, `Skill`, `Document`, `Notification`, `VerificationToken`, `AuditLog`) + `entity/enums/` for all status/role/type enums (`UserRole`, `MembershipRole`, `MembershipStatus`, `OrganizationStatus`, `EventStatus`, `SignupStatus`, `NotificationType`, `TokenType`, etc.). Enum values are the source of truth for state machines — check them before adding a new status.
- `repository/` — Spring Data JPA repositories with custom queries.
- `service/` — business logic, one service per domain (`AuthService`, `OrganizationService`, `MembershipService`, `EventService`, `ProfileService`, `NotificationService`, `AdminService`, `DashboardService`, `DocumentService`, `SkillService`, `EmailService`). Also home to scheduled jobs: `EventCompletionJob` (03:00 UTC, PUBLISHED events past `end_date` → COMPLETED) and `AccountPurgeJob` (04:00 UTC, purges anonymized accounts >30 days old).
- `controller/` — REST controllers, grouped into subpackages by domain (`auth/`, `organization/`, `membership/`, `event/`, `profile/`, `notification/`, `document/`, `skill/`, `admin/`, `dashboard/`). All routes are under `/api/v1/**`.
- `dto/` — request/response records, mirroring the controller subpackage layout, plus shared envelopes: `ApiResponse<T>` (success wrapper), `PageResponse<T>` (pagination), `ErrorResponse` (error wrapper with optional field-level validation errors).
- `exception/` — `GlobalExceptionHandler` (`@RestControllerAdvice`) maps custom exceptions to HTTP responses: `ResourceNotFoundException`→404, `ConflictException`→409, `ForbiddenException`→403, `BusinessRuleException`→400, bean validation failures→400 with per-field details, anything else→500 (logged). Throw the matching custom exception from services rather than building `ResponseEntity` error responses by hand.
- `config/` — `SecurityConfig` (JWT stateless auth, CORS, CSP, endpoint authorization rules), `config/security/` (`JwtTokenProvider`, `JwtAuthenticationFilter`, `RateLimitingFilter`, `@CurrentUser` annotation for injecting the authenticated user into controller methods), `CacheConfig` (Caffeine), `OpenApiConfig`.
- `util/` — `SlugUtil`, `FileStorageUtil`, etc.

### Auth & authorization

- Stateless JWT (`jjwt`), access token 15 min / refresh token 7 days, secret via `JWT_SECRET` env var.
- Password hashing: BCrypt strength 12.
- Route authorization (`SecurityConfig`): `/api/v1/auth/**` public; `GET /api/v1/organizations|events|skills/**` public; `/api/v1/admin/**` requires `PLATFORM_ADMIN` authority; everything else requires authentication. Finer-grained checks (e.g. "is this user an admin of *this* org") happen inside services, not in `SecurityConfig`.
- `RateLimitingFilter` throttles `/auth/**` (used by `test-e2e.ps1`'s self-throttling).
- Roles: platform-level `UserRole` (e.g. `PLATFORM_ADMIN`) vs. per-organization `MembershipRole` (e.g. `ADMIN` of one org) — these are distinct concepts; don't conflate "org admin" with "platform admin".

### Database

- Postgres + Flyway. Migrations in `src/main/resources/db/migration/V1__*.sql` … `V7__seed_data.sql` are sequential and additive — never edit an already-applied migration; add a new `V{n}__description.sql` instead. Mirror any schema change into `src/test/resources/db/h2migration/` for the H2 integration test profile.
- `spring.jpa.hibernate.ddl-auto: validate` in the base config — schema is driven entirely by Flyway migrations, not Hibernate auto-DDL.
- Profiles: `application.yaml` (base/prod defaults), `application-dev.yaml` (local Docker Postgres on 5433, Flyway disabled, MailHog, verbose logging), `application-prod.yaml`, `application-test.yaml` / `application-h2test.yaml` (test profile).

### Email

`EmailService` sends async (`@Async`) Thymeleaf-templated emails (`src/main/resources/templates/email/*.html`) — welcome, verify, password reset/changed, org validated/rejected, membership accepted, event signup confirmed/waitlisted, feedback request. Routed through MailHog in dev, real SMTP in prod (`MAIL_*` env vars).

## Frontend architecture (`ummati-front/src/app/`)

Angular 21, standalone components (no NgModules), lazy-loaded routes via `loadComponent`, Angular Material for UI.

- `app.routes.ts` — all routes, lazy-loaded per page. `authGuard` protects authenticated-only routes; `guestGuard` protects login/register from already-authenticated users (both in `core/guards/auth.guard.ts`).
- `core/services/` — one Angular service per backend domain, mirroring backend controllers 1:1 (`auth.service.ts`, `organization.service.ts`, `membership.service.ts`, `event.service.ts`, `profile.service.ts`, `notification.service.ts`, `document.service.ts`, `skill.service.ts`, `admin.service.ts`, `dashboard.service.ts`). When a backend endpoint changes shape, update the matching frontend service.
- `core/interceptors/auth.interceptor.ts` — attaches the JWT bearer token to outgoing requests and handles refresh.
- `pages/` — one directory per route/feature (`auth/`, `organizations/`, `events/`, `profile/`, `dashboard/`, `admin/`, `notifications/`, `onboarding/`, `settings/`, `home/`). Nested subfolders for feature variants (e.g. `organizations/organization-create`, `organizations/organization-manage`, `events/event-manage`).
- `shared/components/` — reusable presentational components (`avatar`, `empty-state`, `navbar`, `progress-bar`, `skeleton`, `star-rating`).
- SSR entry points: `main.server.ts`, `app.config.server.ts`, `app.routes.server.ts`, `server.ts` (Express server for `serve:ssr:ummati-front`).
- Tests use **Vitest** (not Jasmine/Karma) via `ng test`; specs live alongside the services they test (e.g. `core/services/event.service.spec.ts`).

## Cross-cutting conventions

- API responses are wrapped consistently: `ApiResponse<T>` for single objects/success, `PageResponse<T>` for paginated lists, `ErrorResponse` for failures — frontend services should expect and unwrap these shapes.
- Organizations are addressed by UUID for mutations but by human-readable `slug` for public GET routes (e.g. `GET /api/v1/organizations/{slug}`, frontend route `organizations/:slug`).
- Membership approval, event signup/waitlist promotion, and org validation all trigger both an in-app `Notification` and (for key transitions) an async email — when touching these flows, check `NotificationService`/`EmailService` call sites so both channels stay in sync.
