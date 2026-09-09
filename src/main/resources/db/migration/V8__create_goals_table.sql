-- The `goals` table was never created by any migration. V1-V7 only ever
-- ALTER or CREATE tables that assume `goals` already exists (see V1's
-- comment: "`goals` may not exist yet in every environment since that
-- entity is newer than the others"). The original base schema (trades,
-- days, capitals, assets, etc.) was bootstrapped via `ddl-auto=update`
-- before Flyway was introduced -- but the Goal entity was added after that
-- switch to `ddl-auto=validate`, so nothing ever actually created this
-- table. Shaped to match Goal.java (BaseEntity fields + Goal's own),
-- following the same column-naming convention as trade_steps/trade_insights.

CREATE TABLE goals (
    id                  bigserial    PRIMARY KEY,
    title               varchar(255) NOT NULL,
    category            varchar(255) NOT NULL,
    status              varchar(255) NOT NULL,
    target_value        double precision,
    start_date          timestamp    NOT NULL,
    end_date            timestamp    NOT NULL,
    manual_progress_pct integer,
    created_at          timestamp    NOT NULL DEFAULT now(),
    updated_at          timestamp,
    deleted_at          timestamp,
    notes               text
);

COMMENT ON COLUMN goals.category IS 'GoalCategoryEnum: PROFIT, TRADE_QUALITY, DISCIPLINE, MANUAL';
COMMENT ON COLUMN goals.status IS 'GoalStatusEnum: ACTIVE, ACHIEVED, FAILED, ARCHIVED';
COMMENT ON COLUMN goals.target_value IS 'Target for PROFIT (currency amount) and TRADE_QUALITY (win rate %, 0-100); unused for DISCIPLINE/MANUAL';
COMMENT ON COLUMN goals.manual_progress_pct IS 'Only used for MANUAL goals -- user-entered progress, 0-100';
