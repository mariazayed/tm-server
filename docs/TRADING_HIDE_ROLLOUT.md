# Rollout checklist: hiding trading

How to ship the hide-trading change (branches `feature/hide-trading-phase1` to `phase5`) and how to undo it.
Trading is controlled by one server property, `features.trading.enabled` (default `false`). The dashboard reads it
from `GET /api/v1/features` at startup and fails closed to investing-only if it cannot.

There is no remote flag service in this project, so there is no percentage rollout. The flag is a server property:
changing it means editing the config and restarting the server. For a one-user tracker that is the right size; the
steps below replace a staged rollout.

## 0. Before you start

- [ ] Full database backup (not `scripts/backup-invest-db.sh`, which covers only `invest_*` tables). Trade, capital and
      day data must be in it.
- [ ] Download `GET /api/v1/archive/trades.csv` from the **old** server if it has it, or export trades from the old UI.
      Keep it with the backup.
- [ ] Pause the MT5 importer. While trading is off it gets 404 from `/api/v1/trade` and stops anyway; pausing it avoids
      silent failures in its logs.
- [ ] CI is green on all five branches in both repos (`mvn verify`, `flutter test`, `flutter analyze` as configured).
- [ ] Decide the hosting order below and who is affected (this is a personal app, so just you).

## 1. Deploy order

The dashboard is safe to ship first: against an older server `/features` is a 404, the dashboard treats that as
trading off, and hides trading. The server is safe to ship second. Doing it the other way round breaks the old
dashboard (its home page calls `/trade` and would get 404).

1. [ ] Merge and deploy **tm_dashboard** (phases 1 to 5).
2. [ ] Merge and deploy **tm-server** (phases 1 to 5) with `features.trading.enabled=false` (the default).
3. [ ] Restart the server and confirm in the log that it started without Flyway or Hibernate errors. No migration is
       part of this change.

## 2. Smoke test (server)

```bash
BASE=http://localhost:8080/api/v1
curl -s $BASE/features                     # {"trading":false,"investing":true}
curl -s -o /dev/null -w '%{http_code}\n' $BASE/trade        # 404
curl -s -o /dev/null -w '%{http_code}\n' $BASE/day          # 404
curl -s -o /dev/null -w '%{http_code}\n' $BASE/asset        # 404
curl -s -o /dev/null -w '%{http_code}\n' $BASE/strategies   # 404
curl -s -o /dev/null -w '%{http_code}\n' $BASE/invest/settings          # 200
curl -s -o /dev/null -w '%{http_code}\n' $BASE/goal                     # 200
curl -s -o /dev/null -w '%{http_code}\n' $BASE/archive/trades.csv       # 200
curl -s $BASE/archive/trades.csv | head -3   # header + your past trades
```

## 3. Smoke test (dashboard)

- [ ] Fresh load lands on Invest, not the old home. Opening `/`, `/trades`, `/calendar`, `/trading-hours`,
      `/risk-management`, `/strategies`, `/assets` all end up on `/invest`. Check a bookmark and a hard refresh too.
- [ ] Menu shows: Invest (Overview, Contributions, Journal, Learning, Rules, Ask your journal, Review, Settings),
      Goals, Planner, Decisions, Questions & Answers, Expenses, Others (Deposits & Withdraws, Expense Reasons,
      Reminder Notes, Strict Rules). No Home, Calendar, Trading Hours, Trades, Assets, Risk Management, Strategies.
- [ ] Both layouts: wide window (sidebar) and a narrow one (drawer).
- [ ] Add a contribution, open the journal, change a setting: investing still works end to end.
- [ ] Goals: category picker offers Manual only. Decisions: no Trading category. Old goals and decisions still list.
- [ ] Invest, Settings, "Trading history": the download button opens a CSV with your past trades.
- [ ] Stop the server and reload the dashboard: it still opens (investing-only) and shows the usual load errors on
      data pages, not a blank screen. Allow about 5 seconds at startup.

## 4. What to watch for the first week

There is no analytics or error tracking in these apps, so watch by hand:

- [ ] Server log: no spike of 404s on `/api/v1/trade`, `/day`, `/asset`, `/strategies`, apart from the importer (expected
      until paused) and any old browser tab still open.
- [ ] Server log: no 5xx on `/api/v1/invest/**`.
- [ ] Anything you tried to do and could not. Decide per item: bring it back as an investing feature, or leave it.
- [ ] Contribution rhythm: the month-streak and status cards are the measure of whether the investing focus is working.

## 5. Rollback

Trading data is never changed by this feature, so rollback is configuration only.

1. [ ] Set `features.trading.enabled=true` in the server config (`application-local.properties` or environment
       `FEATURES_TRADING_ENABLED=true`) and restart the server.
2. [ ] Reload the dashboard. It reads the flag at startup, so menu, home page, routes and pickers return to their
       original state. No dashboard redeploy is needed.
3. [ ] Resume the MT5 importer. Overlapping date ranges are safe: trades whose `externalId` already exists are skipped.

If the **dashboard** itself is faulty (not the flag), redeploy the previous build; it works against the new server
because every endpoint it used still exists and answers when trading is on.

## 6. After a stable week or two

- [ ] Decide: keep the flag permanently (opt-in trading later), or delete the dead code (Phase 6).
- [ ] If deleting: move `capital_list.dart` and `capital_stat_card.dart` out of `lib/screens/dashboard` first (the
      Capital screen uses them; see `test/trading_boundary_test.dart`), then remove the trading folders, services,
      models and enums, and the entries in `TradingGateFilter.TRADING_PREFIXES`.
- [ ] Remove the unused `URL_TRADE_PLAN`, `URL_TRADE_EXECUTION`, `URL_TRADE_MONITORING`, `URL_TRADE_TERMINATION` and
      `URL_TRADE_EVALUATION` constants.

## Guards that keep this honest (run in CI)

| Check | Repo | Fails when |
|---|---|---|
| `TradingGateCoverageTest` | tm-server | A controller is added whose path is neither in `TradingGateFilter.TRADING_PREFIXES` nor in the always-available list |
| `TradingGateFilterTest`, `FeaturesController*Test`, `ArchiveControllerTest` | tm-server | The gate, the flag endpoint or the archive export change behaviour |
| `trading_boundary_test.dart` | tm_dashboard | Investing code imports trading code; a trading screen, service or model is used outside the trading folders; a route that builds a trading screen is not covered by the gate |
| `feature_gate_test.dart`, `feature_flags_test.dart`, `menu_investing_only_test.dart` | tm_dashboard | Redirects, flag parsing or the menu change |
