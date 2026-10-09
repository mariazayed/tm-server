# Before this server is reachable from anywhere but your own machine

The server has **no login, by design**. Anyone who can reach port 8080 can read and change everything under
`/api/v1/**`, and can press the AI buttons, which **spend your AI provider's money** (capped at 60 calls a day by default,
but the cap is shared, so a stranger can use it all up) and send your journal text to the provider.

So the rule is: **the network is the lock.** Nothing in this repo checks passwords. Pick one of the options below
*before* you set `ai.enabled=true` on a server that anything else can reach.

## What the server does by default

Out of the box it listens on **every network interface** (checked: with no setting it answers on the machine's LAN
address as well as on `localhost`). Set one environment variable to change that:

```
SERVER_ADDRESS=127.0.0.1     # only this machine can connect (checked: the LAN address then refuses)
```

(`server.address=127.0.0.1` in `application-local.properties` does the same.) Use this in every option below
except "everyone on a trusted LAN".

## Options, safest first

**A. Keep it on your machine (recommended).** `SERVER_ADDRESS=127.0.0.1`. The dashboard already points at
`http://localhost:8080` and the server already allows only `http://localhost:4300` as a browser origin, so this works
today with no other change. To use it from another device, tunnel instead of opening the port:
`ssh -L 8080:127.0.0.1:8080 you@the-server` (run the dashboard on the device that holds the tunnel), or put both
machines on a private network such as Tailscale or WireGuard and bind to that interface's address.

**B. A trusted home or office network only.** Leave the default bind, make sure the router does not forward port 8080,
and do not run it on shared Wi-Fi.

**C. On the internet, behind a reverse proxy that holds the password.** `SERVER_ADDRESS=127.0.0.1` so the only way in is
the proxy, then put HTTPS, a password and (better) an IP allowlist in front. Ready-to-edit examples:

- `deploy/Caddyfile.example` (Caddy: automatic certificates, `basic_auth`, `remote_ip` allowlist)
- `deploy/nginx.conf.example` (nginx: `auth_basic`, `allow`/`deny`, 2 MiB body cap, HTTPS redirect)

Both examples were written for this guide and **not run here** (neither tool is installed in the development
environment); run `caddy validate` or `nginx -t` on your copy first. The password-file command in the nginx example was
checked.

A browser dashboard on a different origin from the API adds two catches with option C, which need a change in the
dashboard (its base URL is fixed to `http://localhost:8080/api/v1`) and in the server's allowed origin, neither of which
is done: the page must be served from the same host as the API (so the browser sends the password with every call and
no cross-origin preflight is needed), or the allowed origin must be changed to the dashboard's. Until then, option A
is the practical way to use it remotely.

**D. A cloud host.** Use the provider's own gate (an identity-aware proxy, a VPN, or a security group that lists only your
IP) in front of the service, and keep the database port private. Never put the database on a public address.

## Checklist before you turn AI on for a reachable server

1. `SERVER_ADDRESS=127.0.0.1` (or a private-network address), unless the machine is on a trusted LAN.
2. Whatever can reach the port is either you, or a proxy/VPN that asks for something you alone have.
3. HTTPS if the traffic crosses any network you do not own (the journal text and your key-holder's bill are at stake).
4. `ai.provider.api-key` is in an environment variable or the untracked `application-local.properties`, never in git.
5. Set the provider's own monthly spending limit as a second ceiling; the app's cap is per day, in this server only.
6. Check `GET /api/v1/invest/ai/usage` after the first day.
7. Swagger UI and `/v3/api-docs` are public like everything else; the proxy covers them too.

## How to check it from outside

From a different device on the network, `curl -i http://<server-ip>:8080/api/v1/invest/ai/status` should **fail to
connect** (options A and C) or return `401`/`403` (option C through the proxy without credentials). A `200` means the lock is open.
