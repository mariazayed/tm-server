-- A history of changes to the plan (AI assistant, step 6). invest_settings is a single row that is
-- overwritten on every save, so without this there is no way to know that the amount, the ticker or the
-- buy day was ever different. One row per field that actually changed; created_at is when it changed.
--
-- Only the three fields the plan rule is about are recorded. Values are text so one column fits an amount
-- in cents, a ticker and a day of the month.

CREATE TABLE invest_settings_changes (
    id         bigserial   PRIMARY KEY,
    field_name varchar(32) NOT NULL,
    old_value  varchar(64),
    new_value  varchar(64) NOT NULL,
    created_at timestamp   NOT NULL DEFAULT now(),
    updated_at timestamp,
    deleted_at timestamp,
    notes      text
);

CREATE INDEX idx_invest_settings_changes_created ON invest_settings_changes (created_at);

COMMENT ON COLUMN invest_settings_changes.field_name IS 'MONTHLY_AMOUNT (cents), TICKER or BUY_DAY';
