-- Phase 1: performance-tracking / analysis fields on `trades`, per the recorded
-- plan (Automatic / Computed / AI-suggested / Manual tiers). All nullable --
-- manual entries and MT5-imported trades fill different subsets, and existing
-- rows have none of this history yet. Entity mapping (Trade.java, DTOs, the
-- review endpoint) is Phase 2; this migration only shapes the schema.
--
-- Enum-backed columns are varchar(255) to match Hibernate's default
-- @Enumerated(EnumType.STRING) column length, so Phase 2 can map them with no
-- @Column(length=...) surprises at ddl-auto=validate time.

ALTER TABLE trades
    -- Source & traceability
    ADD COLUMN external_id             varchar(64),
    ADD COLUMN source                  varchar(255),
    ADD COLUMN reviewed_at             timestamp,

    -- Setup context
    ADD COLUMN session                 varchar(255),
    ADD COLUMN day_of_week             varchar(255),
    ADD COLUMN volatility              varchar(255),
    ADD COLUMN trade_type              varchar(255),
    ADD COLUMN market_state            varchar(255),
    ADD COLUMN market_bias             varchar(255),
    ADD COLUMN entry_reason            varchar(255),
    ADD COLUMN entry_confirmation      varchar(255),
    ADD COLUMN entry_zone_type         varchar(255),
    ADD COLUMN probability             varchar(255),

    -- Risk & sizing
    ADD COLUMN risk_percent            double precision,
    ADD COLUMN account_balance_at_entry double precision,
    ADD COLUMN planned_risk_reward     double precision,
    ADD COLUMN r_multiple              double precision,
    ADD COLUMN scaling_rule            varchar(255),

    -- Trade management & outcome
    ADD COLUMN mae                     double precision,
    ADD COLUMN mfe                     double precision,
    ADD COLUMN exit_reason             varchar(255),
    ADD COLUMN invalidation_condition  varchar(255),
    ADD COLUMN sl_placement_reason     varchar(255),
    ADD COLUMN tp_placement_reason     varchar(255),

    -- Discipline & psychology (manual; discipline_score reuses the ScoreEnum
    -- scale already used by days.psychology_rate)
    ADD COLUMN followed_plan           boolean,
    ADD COLUMN discipline_score        varchar(255);

COMMENT ON COLUMN trades.external_id IS 'Broker-side identifier (e.g. MT5 position_id) used to dedupe re-imports; null for manually-entered trades';
COMMENT ON COLUMN trades.source IS 'MANUAL or AUTO_IMPORTED';
COMMENT ON COLUMN trades.reviewed_at IS 'Set once a person has confirmed/edited the AI-suggested and manual fields; null means still pending review';
COMMENT ON COLUMN trades.mae IS 'Max Adverse Excursion: worst price move against the position while it was open';
COMMENT ON COLUMN trades.mfe IS 'Max Favorable Excursion: best price move in favor of the position while it was open';
COMMENT ON COLUMN trades.r_multiple IS 'Realized P&L expressed in multiples of the initial risk amount';
COMMENT ON COLUMN trades.planned_risk_reward IS 'Approximated from the initial SL/TP order prices at entry, not a guaranteed reflection of intent';

-- external_id uniqueness only matters when present. Postgres already treats
-- every NULL as distinct under a plain UNIQUE constraint, but a partial index
-- (skipping NULL rows) keeps the index smaller once most manually-entered
-- trades never set this column.
CREATE UNIQUE INDEX ux_trades_external_id ON trades (external_id) WHERE external_id IS NOT NULL;

-- Mistake tags: a trade can carry more than one, so this is its own table
-- rather than a column, mirroring the @ElementCollection pattern already used
-- for strategies.tags. Column names are deliberate -- Phase 2 maps them
-- explicitly via @CollectionTable/@Column rather than relying on Hibernate's
-- default naming, so this exact shape must be preserved.
CREATE TABLE trade_mistake_tags (
    trade_id    bigint       NOT NULL REFERENCES trades (id),
    mistake_tag varchar(255) NOT NULL
);

CREATE INDEX ix_trade_mistake_tags_trade_id ON trade_mistake_tags (trade_id);
