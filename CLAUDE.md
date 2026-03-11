# Runnit Backend — Claude Instructions

## Stack
- Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA
- MySQL (PlanetScale in prod, local MySQL for dev)
- Flyway migrations, Lombok, Stripe Java SDK, jsoup

## Running Locally
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Local profile: `src/main/resources/application-local.properties`
- Points to `localhost:3306/runnit_db`
- Flyway disabled (run migrations manually or via PlanetScale console)

## Key Packages
- `controller/`  — REST endpoints (`/api/**`)
- `model/`       — JPA entities
- `repository/`  — Spring Data repositories
- `service/`     — Business logic
- `security/`    — JWT filter + JwtUtil
- `config/`      — SecurityConfig (CORS, permitAll list), AppConfig

## Auth
- JWT in `Authorization: Bearer <token>` header
- `JwtAuthenticationFilter` sets `auth.getPrincipal()` = `Long userId`
- Public endpoints in `SecurityConfig.permitAll()`: `/api/auth/**`, `/api/billing/webhook`, `/api/integrations/strava/callback`, etc.

## Database Rules (PlanetScale/Vitess)
- **No `IF NOT EXISTS`** in ALTER TABLE — Vitess doesn't support it
- **No multi-column ALTER TABLE** when any column might already exist — run each ADD COLUMN separately
- **No foreign key constraints** — PlanetScale doesn't enforce them
- Migrations live in `src/main/resources/db/migration/` — next is V19
- To apply to prod: run each statement individually in PlanetScale console

## Known Schema State (after V18)
Users table has all columns including: strava_*, garmin_*, bio, role, is_public, onboarding_complete, unit_system, user (alias col)
- `strava_token_expires_at` = BIGINT (epoch seconds, maps to Java Long)
- `garmin_token_expires_at` = TIMESTAMP (in DB from V1, not mapped in entity — ignore)
- `spotify_*` columns exist in DB but not in entity — ignore

## Reaction Enum
`Reaction.ReactionType` = `LIKE, FIRE, CLAP` — must match DB ENUM exactly

## Environment Variables
Set in `.env` (git-ignored). See `.env.example`.
- `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_*`
- DB credentials via `DB_HOST`, `DB_USER`, `DB_PASSWORD`

## Common Pitfalls
- `loginWithEmail` catches ALL exceptions and returns 401 — any DB error looks like bad credentials
- Repository derived queries: use `user_Id` (underscore) for @ManyToOne traversal, e.g. `countByUser_IdAndReadFalse`
- `BillingController`: use fully qualified `com.stripe.param.checkout.SessionCreateParams` and `com.stripe.param.billingportal.SessionCreateParams` — both have the same short name
