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

`InvestSchemaTest`, `ContributionServiceTest` and `PriceAndPerformanceServiceTest` apply V22 to a scratch database and checks the entities with Hibernate's
`validate`, plus the constraints. It runs only when `INVEST_TEST_DB_URL` is set (CI does this) and
**drop and recreate the `invest_*` tables** in that database:

```bash
INVEST_TEST_DB_URL=jdbc:postgresql://localhost:5432/scratch INVEST_TEST_DB_USER=postgres \
  mvn test -Dtest=InvestSchemaTest
```

## Contribution API (all routes need the login token)

| Method and path | Purpose |
|---|---|
| `GET /api/v1/invest/settings` | Plan settings (amount, currency, buy day, ticker, start date) |
| `GET /api/v1/invest/contributions` | List, newest first |
| `POST /api/v1/invest/contributions` | Create (201) |
| `GET/PUT/DELETE /api/v1/invest/contributions/{id}` | Read, replace, soft-delete |
| `GET /api/v1/invest/contributions/status` | Month-by-month status, streaks, next due date |
| `GET /api/v1/invest/contributions/export` | CSV download |
| `POST /api/v1/invest/contributions/import` | CSV body (`text/csv`); 200, or 422 with row errors |

Request body: `contributedOn` (`YYYY-MM-DD`), `amountCents`, `shares`, `price`, optional `feeCents`
and `notes`. Rules: positive amount, shares and price; the date cannot be in the future (judged in
the settings timezone); `shares` must be within 1% of `amount / price` to allow broker rounding.

**Status.** A month is `DONE` with at least one buy, `MISSED` if it ended without one, and for the
current month `PENDING` (buy day not reached) or `OVERDUE`. Pending and overdue never break the
streak, so it only resets once a month actually ends empty.

**CSV.** Columns `date, amount, shares, price, fee, notes`, with amount and fee in currency units
(`300.00`). Headers are case-insensitive and accept aliases (`trade date`, `total`, `quantity`,
`commission`, ...); `$` and thousands separators are tolerated. Import is all-or-nothing, reports
every bad row with its line number, and skips rows identical to an existing contribution, so
re-importing the same file is safe. Exported notes that start with `=`, `+`, `-` or `@` get a
leading `'` so spreadsheets do not run them as formulas.

## Prices and performance

| Method and path | Purpose |
|---|---|
| `POST /api/v1/invest/prices/refresh` | Fetch fresh closes now. A provider failure comes back in the body (`error`), not as an HTTP error |
| `GET /api/v1/invest/performance?range=ALL` | Totals plus a chart series. `range` is `3M`, `6M`, `1Y` or `ALL` and only trims the series |

**Price source.** `PriceProvider` is an interface; `YahooPriceProvider` reads daily closes from Yahoo
Finance's key-less chart endpoint (unofficial, so it can change or rate-limit). Raw closes are used,
not dividend-adjusted ones, so they compare with the price you paid. Closes are cached in
`invest_prices`; the first refresh backfills from your earliest contribution, later ones fetch from
the newest cached close minus 5 days, which also corrects late revisions. A failed fetch never
touches the cache, and `performance` keeps working from it.

**Daily job.** Off by default. Set `invest.prices.enabled=true` (cron via `invest.prices.refresh-cron`,
UTC, default 23:30) to refresh automatically.

**Numbers.** *Invested* is cash out of pocket: amounts plus fees. *Average cost* is invested divided
by shares, so it includes fees. *Value* is shares times the latest close. *Gain %* is gain divided by
invested. `priceAsOf` says which day the price is from; `stale` is true when it is more than 4 days
old, and `priceFromCache` is false when no closes exist yet and your last buy price is used instead.
The series has one point per trading day from your first buy (cumulative invested and value); before
any prices exist it has one point per buy, valued at the price you paid.

## Deployment

Any host that runs Java 17 and PostgreSQL. Terminate HTTPS in front of the app (Caddy, nginx or a
cloud load balancer); bearer tokens must never travel over plain HTTP. Set the two secrets in the
host's secret store.

## CI

`.github/workflows/ci.yml` runs `mvn verify` with a PostgreSQL service. `TmServerApplicationTests.contextLoads`
is skipped in CI because it needs a fully migrated local database.
