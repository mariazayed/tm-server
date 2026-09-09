-- Phase 1: event log for what happened *during* a trade (SL moved to
-- break-even, trailed, scaled in/out, etc.) -- a single Trade row can't
-- capture a sequence of events, and that sequence is itself useful analysis
-- material (see the recorded plan). Auto-populated in Phase 2+ by diffing
-- MT5's order-modification history per position; manual trades can log steps
-- too. Shaped like every other entity (id/created_at/updated_at/deleted_at/
-- notes) so Phase 2's TradeStep can simply extend BaseEntity.

CREATE TABLE trade_steps (
    id             bigserial    PRIMARY KEY,
    trade_id       bigint       NOT NULL REFERENCES trades (id),
    type           varchar(255) NOT NULL,
    occurred_at    timestamp    NOT NULL,
    price_at_event double precision,
    notes          text,
    created_at     timestamp    NOT NULL DEFAULT now(),
    updated_at     timestamp,
    deleted_at     timestamp
);

COMMENT ON COLUMN trade_steps.type IS 'TradeUpdateEnum (extended in Phase 2 beyond MOVE_SL_TO_BE -- e.g. PARTIAL_CLOSE, SCALE_IN, SCALE_OUT, SL_MODIFIED, TP_MODIFIED)';
COMMENT ON COLUMN trade_steps.occurred_at IS 'When the event happened on the broker side, not when this row was inserted (created_at)';

CREATE INDEX ix_trade_steps_trade_id ON trade_steps (trade_id);
