-- Phase 1: AI-generated narrative review of a trade, kept separate from the
-- trade's own data so it's versionable and never silently overwrites a
-- person's own notes or confirmed fields. A trade can accumulate more than
-- one insight over time (re-run after a prompt change) rather than being
-- overwritten in place. Same BaseEntity shape as trade_steps.

CREATE TABLE trade_insights (
    id                     bigserial    PRIMARY KEY,
    trade_id               bigint       NOT NULL REFERENCES trades (id),
    model                  varchar(100) NOT NULL,
    prompt_version         varchar(50)  NOT NULL,
    summary                text,
    suggested_improvement  text,
    detected_mistake_tags  text,
    confidence             double precision,
    generated_at           timestamp    NOT NULL,
    notes                  text,
    created_at             timestamp    NOT NULL DEFAULT now(),
    updated_at             timestamp,
    deleted_at             timestamp
);

COMMENT ON COLUMN trade_insights.model IS 'Which model/version produced this insight, e.g. claude-sonnet-5';
COMMENT ON COLUMN trade_insights.prompt_version IS 'Lets later insights be compared against earlier ones after the prompt changes';
COMMENT ON COLUMN trade_insights.detected_mistake_tags IS 'AI-detected candidates as a JSON-encoded array string (e.g. ["REVENGE_TRADE"]), distinct from the person-confirmed trade_mistake_tags table';

CREATE INDEX ix_trade_insights_trade_id ON trade_insights (trade_id);
