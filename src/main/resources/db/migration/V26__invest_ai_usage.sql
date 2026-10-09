-- Per-day AI call and token counts (AI assistant, step 1). Not owner data: it only lets the daily
-- spending cap survive a restart and lets the settings screen show today's usage. Never exported.
--
-- One row per (UTC day, feature). Calls are counted before the model is called, so a failed or
-- cancelled call still counts against the cap.

CREATE TABLE invest_ai_usage (
    id            bigserial   PRIMARY KEY,
    usage_date    date        NOT NULL,
    feature       varchar(32) NOT NULL,
    calls         integer     NOT NULL DEFAULT 0 CHECK (calls >= 0),
    input_tokens  bigint      NOT NULL DEFAULT 0 CHECK (input_tokens >= 0),
    output_tokens bigint      NOT NULL DEFAULT 0 CHECK (output_tokens >= 0),
    created_at    timestamp   NOT NULL DEFAULT now(),
    updated_at    timestamp,
    CONSTRAINT uq_invest_ai_usage UNIQUE (usage_date, feature)
);
