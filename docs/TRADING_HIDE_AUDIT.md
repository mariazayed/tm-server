# Phase 0 audit: hiding trading, keeping investing

Scope: `tm-server` (Spring Boot, `trademind.tm_server`) and `tm_dashboard` (Flutter, go_router).
Method: static read of both repos at the current default branch. Nothing was run or changed.

Legend: **HIDE** = trading-only. **REFRAME** = shared, needs trading wording or data removed. **KEEP** = investing, untouched.

## 1. Headline findings

1. **Investing is already isolated.** Server `invest/**` imports only generic infrastructure (`AppConstants`, `BaseEntity`,
   exceptions, `ai.LlmClient`/`LlmResult`/`LlmOptions`) and three shared enums (`JournalEntryType`, `SourceStatus`,
   `SourceType`). No non-invest class imports `invest`. In the dashboard, `screens/invest`, `models/invest` and the invest
   services import only generic widgets, theme and `route_names`. Hiding trading therefore cannot break investing.
2. **Hiding is almost entirely routing, navigation and one screen.** The dashboard has one `ShellRoute` and one menu
   list (`widgets/menu_bar.dart`, `_menus()`), shared by the desktop sidebar and the mobile drawer.
3. **The home route `/` is the trading dashboard.** `DashboardScreen` loads trades, capital, expenses, notes and rules.
   Invest lives at `/invest`. Home must change (see section 3).
4. **There is no authentication and no feature gating on the server.** `docs/INVESTMENT_TRACKER.md` states every route is
   open. So hiding the UI alone does not stop trading endpoints, and a server-side switch is needed (Phase 1/2).
5. **The shared AI client is used by both sides.** `ai/AiSuggestionService` (trade suggestions) and `invest/ai/*` both use
   `LlmClient` and the same API key slot. Only the trade suggestion service is trading-specific; the client must stay.
6. **Careful with grep.** The package name `trademind` matches "trade", so server counts for "trade" are meaningless.
   The tables below come from reading controllers, entities and routes instead.

## 2. Server inventory

### 2.1 Controllers and routes (`AppConstants`)

| Route prefix | Controller | Verdict | Notes |
|---|---|---|---|
| `/api/v1/invest/**` (contributions, journal, learning, rules, performance, export, `ai/*`) | `invest/*`, `auth/InvestController` | **KEEP** | Core of the product. |
| `/api/v1/trade`, `/tradeStep`, `/tradeInsight` | `TradeController`, `TradeStepController`, `TradeInsightController` | **HIDE** | Trade journal. `TradeController` also hosts the AI suggest endpoint (`SUB_URL_SUGGEST`). |
| `/api/v1/day` | `DayController` | **HIDE** | Trading calendar days. |
| `/api/v1/strategies` | `StrategyController` | **HIDE** | Trade strategies (entity owns trade-related children). |
| `/api/v1/asset` | `AssetController` | **HIDE** | Trading instruments (pip size, contract size). Invest uses its own ticker in `invest_settings`. |
| `/api/v1/staticData` | `StaticDataController` | **REFRAME** | Serves the trade enums (entry reasons, sessions, etc.). Dashboard `static_data_service` also fetches it. Keep the endpoint, drop trade lookups once trading is off. |
| `/api/v1/capital` | `CapitalController` | **REFRAME** | Deposits/withdrawals plus an imported P&L row per trade. Decide whether investing wants a cash ledger. |
| `/api/v1/goal` | `GoalController` | **REFRAME** | Generic, but `GoalCategoryEnum` has `TRADE_QUALITY`. |
| `/api/v1/decision` | `DecisionController` | **REFRAME** | Generic, but `DecisionCategoryEnum` has `TRADING` next to `INVESTING`. |
| `/api/v1/planner` | `PlannerController` | **REFRAME** | Weekly planner. Only copy mentions trading. |
| `/api/v1/expense`, `/expenseReason` | `ExpenseController`, `ExpenseReasonController` | **HIDE** | Trading expenses and their reason list. |
| `/api/v1/questionAnswers`, `/questionCategories` | `QuestionAnswer*`, `QuestionCategory*` | **KEEP** | Generic. |
| `/api/v1/reminderNotes`, `/strictRules` | `ReminderNote*`, `StrictRule*` | **KEEP** | Generic. Confirm content is not trading rules. |

Related unused constants: `URL_TRADE_PLAN`, `URL_TRADE_EXECUTION`, `URL_TRADE_MONITORING`, `URL_TRADE_TERMINATION`,
`URL_TRADE_EVALUATION` have no controller. They are dead and can be removed in cleanup.

### 2.2 Background and AI

| Item | Verdict | Notes |
|---|---|---|
| `ai/AiSuggestionService`, `AiSuggestionPromptBuilder`, `AiSuggestionResponseParser`, `AiSuggestion`, config `ai.suggestions.enabled` | **HIDE** | Trade analysis suggestions. Already off by default. |
| `ai/LlmClient`, `AnthropicLlmClient`, `LlmOptions`, `LlmResult`, `LlmException` | **KEEP** | Shared by `invest/ai`. |
| `invest/price/PriceRefreshJob`, `invest/reminder/ReminderJob`, `invest/backup/BackupJob` | **KEEP** | All `@ConditionalOnProperty`. No trading scheduled jobs exist. |
| `TradeEnrichmentService` and related | **HIDE** | Computed fields on trades (`recomputeEnrichment`). |

### 2.3 Data (Flyway)

Trading-owned migrations: `V1` (enum columns), `V2` (trade analysis), `V3` (trade steps), `V4` (trade insights),
`V5` (unique mistake tags), `V6`/`V15` (asset contract and pip size), `V7` (detected structures), `V9` (capital external id),
`V16` (day important). Investing: `V22`-`V29`, all tables prefixed `invest_`. Shared: `V8` goals, `V10`-`V13` decisions,
`V14` expenses, `V17`/`V18` Q&A, `V19`-`V21` planner.

**Decision for Phase 1: do not drop or alter any table.** Hide at the access layer only. Tables stay for history and
for a possible opt-in return.

### 2.4 Tests

Trading-only tests: `trade/TradeServiceTest`, `trade/TradeEnrichmentServiceTest`, `ai/AiSuggestion*Test`,
`ai/AnthropicLlmClientTest` (**KEEP**, tests the shared client). `staticData/StaticDataTest` is **REFRAME**.
All `invest/**` tests are **KEEP** and must stay green. CI is `.github/workflows/ci.yml` (runs the invest schema tests
against a scratch DB).

## 3. Dashboard inventory

### 3.1 Routes (`lib/configs/routes.dart`, `lib/constants/route_names.dart`)

| Route name | Path | Verdict |
|---|---|---|
| `home` | `/` (trading `DashboardScreen`) | **REFRAME**: becomes the investing home, or redirects to `/invest`. |
| `invest` and 7 children (`rules`, `settings`, `learning`, `journal`, `ask`, `review`, `contributions`) | `/invest/**` | **KEEP** |
| `calendar` | `/calendar` | **HIDE** (trading days) |
| `tradingHours` | `/trading-hours` | **HIDE** |
| `trades` | `/trades` (merged trade list + journal) | **HIDE** |
| `riskManagement` | `/risk-management` | **HIDE** (position-size and equity-curve metrics) |
| `strategies` | `/strategies` | **HIDE** |
| `assets` | `/assets` | **HIDE** |
| `capital` | `/capital` ("Deposits & Withdraws") | **REFRAME** |
| `goals` | `/goals` | **REFRAME** (`TRADE_QUALITY` category) |
| `decisions` | `/decisions` | **REFRAME** (`TRADING` category) |
| `planner` | `/planner` | **REFRAME** (example copy) |
| `expenses`, `expenseReasons` | various | **HIDE** | Trading expenses and their reason list. |
| `questionsAnswers`, `reminderNotes`, `strictRules` | various | **KEEP** |

All sub-routes are children of `/`, so one redirect in `GoRouter(redirect: ...)` can cover every hidden path, including
deep links and web URLs.

### 3.2 Navigation (`lib/widgets/menu_bar.dart`, `_menus()`)

Single list drives both `_ModernSidebar` and `_ModernDrawer`. Hide: **Calendar, Trading Hours, Trades**, and under
"Others": **Assets, Risk Management, Strategies**. Reframe: **Deposits & Withdraws**, **Goals**, **Decisions**,
**Planner**. App wordmark "TM 🧠" appears in three places (sidebar, drawer, appbar).

### 3.3 Home dashboard (`lib/screens/dashboard/`)

`DashboardScreen` composes these sections. All trade-derived pieces are **HIDE**; the rest can be reused on the investing home.

| Widget | Source data | Verdict |
|---|---|---|
| `SummarySection`, `CapitalGrowthChart`, `CapitalList` | capital + trades | **REFRAME** (depends on whether capital stays) |
| `WeeklyPnlChart`, `WeeklyHistoryTable`, `weekly_pnl_table` | trades | **HIDE** |
| `TradeStatsRow`, `trade_stat_tile` | trades | **HIDE** |
| `StrategyPerformanceChart`, `SessionPerformanceChart`, `MistakeTagChart` | trades | **HIDE** |
| `ExpenseSummaryChart` | expenses | **KEEP** |
| `GoalsProgressSection`, `PlannerDashboardSection` | goals / planner | **KEEP** (after goal category change) |
| `DashboardNoteStrip` | reminder notes, strict rules | **KEEP** |
| `classes/trade_analytics_helper.dart` | trades | **HIDE** |

### 3.4 Other trading code

- Screens: `calendar/`, `trade_journal/`, `trade_list/`, `trade_review/`, `trading_hours/`, `risk_management/`, `strategies/`, `assets/`.
- Services: `trade_service`, `trade_calculator_service`, `strategy_service`, `asset_service`, `day_service`, `memoir_service`, `static_data_service`.
- Models: `trade_model`, `day_model`, `strategy_model`, `asset_model`, `memoir_model`, plus the 25 trade enums and matching `*_model.dart` lookup models.
- Providers in `main.dart`: `CalendarCubit` is trading-only; `ThemeCubit` is shared.
- Platform and copy: `web/index.html` and `web/manifest.json` say "Trade journal and personal investing tracker."; `main.dart` title "TM Dashboard".
- Tests that touch trading: `trade_calculator_service_test`, `trade_group_card_test`, `risk_management/risk_metrics_test`, `dashboard_layout_test`, `mobile_layout_test`. `api_client_test` and `service_cache_test` use trading endpoints as fixtures only (**REFRAME**: swap fixtures to invest endpoints).
- `lib/screens/invest/*` and `test/invest_*_test.dart` (12 files): **KEEP**.

## 4. Shared coupling that needs a decision

| Question | Why it matters | Default recommendation |
|---|---|---|
| What is the new home page (`/`)? | Today it is the trading dashboard. | Redirect `/` to `/invest` now; build an investing home later. |
| Keep `capital` (deposits/withdrawals)? | It also imports one P&L row per trade. | Keep the screen, stop showing PROFIT/LOSS rows when trading is off. |
| Keep `goal` category `TRADE_QUALITY`, `decision` category `TRADING`? | They leak trading into shared forms. | Filter these values out of the pickers when trading is off. Keep existing rows readable. |
| Existing trade, day, strategy, asset data | Historical records and tax history. | Leave in place. No migration, no deletes. |
| Server-side gating | No auth, so hidden UI is still reachable by URL. | Add a `features.trading.enabled` property and a filter returning 404 for the trading prefixes (Phase 2). |
| Same repo or fork for investing-only? | Flag vs separate build. | Flag in the same repo (reversible, one codebase). |

## 5. Hide list (input to Phase 1 and 2)

**Server route prefixes to gate:** `/trade`, `/tradeStep`, `/tradeInsight`, `/day`, `/strategies`, `/asset`. Optionally
`/staticData` trade lookups. Do not gate `/capital`, `/goal`, `/decision`, `/planner` (reframe instead).

**Dashboard paths to guard:** `/calendar`, `/trading-hours`, `/trades`, `/risk-management`, `/strategies`, `/assets`.

**Dashboard menu items to remove:** Calendar, Trading Hours, Trades, Assets, Risk Management, Strategies.

**Home:** replace `DashboardScreen` trade sections.

## 6. Risks found

- No auth means the gate is the only protection for hidden endpoints. Make it server-side.
- `static_data_service` loads trade lookups at startup in the dashboard. If those endpoints are gated, that call must be
  conditional or the app will show errors on launch.
- `capital_service` and `dashboard_screen` fetch trades. Gating `/trade` will break the home dashboard until it is replaced
  or redirected, so ship the redirect and the server gate together.
- Several dashboard tests (`dashboard_layout_test`, `mobile_layout_test`) construct the trading dashboard and will need updating.
- Strategy and Asset entities own trade children (`@OneToMany` cascade). Do not delete them through the gated API.

## 7. Open questions for you

1. Redirect `/` to `/invest`, or build a new investing home now?
2. Keep Capital, Goals, Decisions and Planner as shared tools (reframed), or hide them too?
3. Should trading return later as an opt-in setting, or is this permanent? (Permanent means remove the dead code in Phase 6.)

---

# Phase 4: edge cases and compliance (findings and decisions)

## What switching trading off does to existing data and integrations

| Case | What happens | Decision / action |
|---|---|---|
| **Open positions or orders** | The app stores closed, imported trades only (entry and exit fields, `pnl`); it has no order or position state and does not place orders. Nothing can be stranded open. | No migration needed. |
| **Existing trade history** | Rows stay in the database untouched. Every `/trade`, `/tradeStep`, `/tradeInsight`, `/day`, `/strategies`, `/asset` route is a 404 while the flag is off, including the reads. | Added `GET /api/v1/archive/trades.csv` (read-only, outside the gated prefixes) so history stays reachable for statements and tax records. The dashboard links to it from Invest, Settings, "Trading history". |
| **MT5 importer** | It POSTs to `/api/v1/trade`, so it gets 404 and stops importing while trading is off. Capital P&L rows stop appearing with it. | Intended. Re-importing after the flag is turned back on is safe: `TradeService.filterNewRequests` drops positions whose `externalId` already exists. Stop or pause the importer job rather than letting it fail quietly. |
| **Capital (deposits and withdrawals)** | Stays available. The screen only loads DEPOSIT and WITHDRAW rows; old PROFIT/LOSS rows are not shown. | No change. |
| **Goals and decisions created earlier** | Trade-derived goal categories and the TRADING decision category are hidden from pickers and filters only. Existing rows still list and can be edited. | No change. |
| **Backups and exports** | `/invest/export` covers investing data only; it never included trade data. Trade data is covered by the new CSV and by database backups (`scripts/backup-invest-db.sh` covers the `invest_*` tables only, so use a full database backup for trade history). | Documented. |
| **Turning trading back on** | Set `features.trading.enabled=true` and restart the server. Everything reappears; no data changed. | Documented. |

## Compliance and legal

- This is a personal tracker: it holds no brokerage connection, executes no orders, and the repo contains no disclosure,
  suitability or licensing text to keep or remove. Nothing regulatory was found to preserve.
- If the app is ever offered to other people, the investing screens would need their own review (performance figures are
  shown without a disclaimer, and the AI features produce text about money). That is outside this change.
- The archive CSV contains personal financial records and the endpoint is unauthenticated like the rest of the API (see
  `docs/INVESTMENT_TRACKER.md`, Auth). Keep the server on a trusted network.

## Accessibility and localisation

- The app has no localisation layer; all strings are English literals, so no translation work is created.
- Menu and settings additions use standard Material widgets, labelled by text, so they get the default screen-reader
  labels.

## Not done / left open

- A widget test for the Settings "Trading history" section (needs the fake plan service used by the other settings tests).
- The importer itself lives outside these repos; pausing it is a manual step.
