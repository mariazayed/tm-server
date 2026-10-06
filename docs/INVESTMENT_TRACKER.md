# Investing tracker (server)

Personal tracker for a simple plan. This file is the source of truth for the strategy.

## Strategy

- **Asset:** VT (Vanguard Total World Stock ETF) only
- **Amount:** $300 every month
- **Buy day:** _set me (e.g. the 1st)_
- **Rules:**
  1. Never sell in a crash.
  2. Never change the plan without writing the reason in the journal first.
  3. Wait 72 hours before any deviation.

## Auth

Single owner. `POST /api/v1/auth/login` checks the password against a BCrypt hash and returns a
signed HS256 token (`Authorization: Bearer <token>`). Protected routes:

- `GET /api/v1/auth/me`
- everything under `/api/v1/invest/**` (all future tracker endpoints go here)

The existing trade-journal endpoints are unchanged and remain open. Login is rate limited to 5
attempts per 15 minutes per client address (in memory, single instance). If the settings below are
missing, login returns 503 and the protected routes stay closed.

### Configuration

| Property (env var)                                    | Meaning                                  |
| ----------------------------------------------------- | ---------------------------------------- |
| `app.auth.password-hash` (`APP_AUTH_PASSWORD_HASH`)   | BCrypt hash of your password             |
| `app.auth.session-secret` (`APP_AUTH_SESSION_SECRET`) | 32+ byte random signing secret           |
| `app.auth.session-days` (`APP_AUTH_SESSION_DAYS`)     | Token lifetime, 1-90 days (default 14)   |

Generate them (never commit real values; use `application-local.properties` or env vars):

```bash
python3 -c 'import bcrypt,getpass;print(bcrypt.hashpw(getpass.getpass().encode(),bcrypt.gensalt(12)).decode())'
# or: htpasswd -bnBC 12 "" 'your long password' | tr -d ':\n'
openssl rand -base64 48
```

## Data model (Flyway `V22__create_invest_tables.sql`)

All tables are prefixed `invest_`, so they never collide with the trade-journal schema. Money is
stored as integer cents; share counts and per-share prices are exact decimals. Tables built on
`BaseEntity` also carry `created_at`, `updated_at`, `deleted_at` (soft delete) and `notes`.

| Table | Purpose | Key columns |
|---|---|---|
| `invest_settings` | Single settings row (id pinned to 1) | `monthly_amount_cents`, `currency`, `buy_day` (1-28), `ticker`, `start_date`, `timezone` |
| `invest_contributions` | One row per monthly buy | `contributed_on`, `amount_cents`, `shares`, `price`, `fee_cents` |
| `invest_prices` | Cached daily closes (no soft delete) | `ticker`, `price_date`, `close`; unique per ticker and date |
| `invest_tags` | Shared tag list | `name` (unique, case-insensitive, among live rows) |
| `invest_journal_entries` | Check-ins, quick notes, decisions | `entry_type`, `mood` (1-5), `body` (markdown), optional `contribution_id` |
| `invest_journal_entry_tags` | Entry-to-tag link | `entry_id`, `tag_id` |
| `invest_rules` | The written strategy rules | `text`, `sort_order` |
| `invest_sources` | Reading and watch list | `title`, `source_type`, `url`, `status` |
| `invest_learning_notes` | Notes on what you learn | `title`, `body` (markdown), optional `source_id` |
| `invest_learning_note_tags` | Note-to-tag link | `note_id`, `tag_id` |

The migration seeds the settings row ($300, USD, VT, buy day 1), the tags `emotion`, `learning`,
`market-event` and `decision`, and your three rules. Entities and repositories live in
`trademind.tm_server.invest`.

Sample data for development is in `src/main/resources/db/dev/invest-seed.sql` (not a migration):
`psql -d tm_db -f src/main/resources/db/dev/invest-seed.sql`. It wipes the invest data tables
first, so never run it against real data.

### Schema test

`InvestSchemaTest` applies V22 to a scratch database and checks the entities with Hibernate's
`validate`, plus the constraints. It runs only when `INVEST_TEST_DB_URL` is set (CI does this) and
**drops and recreates the `invest_*` tables** in that database:

```bash
INVEST_TEST_DB_URL=jdbc:postgresql://localhost:5432/scratch INVEST_TEST_DB_USER=postgres \
  mvn test -Dtest=InvestSchemaTest
```

## Deployment

Any host that runs Java 17 and PostgreSQL. Terminate HTTPS in front of the app (Caddy, nginx or a
cloud load balancer); bearer tokens must never travel over plain HTTP. Set the two secrets in the
host's secret store.

## CI

`.github/workflows/ci.yml` runs `mvn verify` with a PostgreSQL service. `TmServerApplicationTests.contextLoads`
is skipped in CI because it needs a fully migrated local database.
