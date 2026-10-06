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

## Deployment

Any host that runs Java 17 and PostgreSQL. Terminate HTTPS in front of the app (Caddy, nginx or a
cloud load balancer); bearer tokens must never travel over plain HTTP. Set the two secrets in the
host's secret store.

## CI

`.github/workflows/ci.yml` runs `mvn verify`. `TmServerApplicationTests.contextLoads` is skipped in
CI because it needs a fully migrated local PostgreSQL.
