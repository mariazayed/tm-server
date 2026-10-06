-- Investing tracker (VT ETF, monthly contributions, journal, learning notes).
-- All tables are prefixed invest_ so they never collide with the trade-journal schema.
-- Money amounts are integer minor units (cents); share counts and per-share prices are exact
-- decimals -- never floating point. Tables mapped from BaseEntity carry its columns
-- (created_at, updated_at, deleted_at, notes); deleted_at is a soft delete.

-- Single settings row (id is pinned to 1).
CREATE TABLE invest_settings (
    id                   bigint       PRIMARY KEY CHECK (id = 1),
    monthly_amount_cents bigint       NOT NULL CHECK (monthly_amount_cents > 0),
    currency             varchar(3)   NOT NULL,
    buy_day              smallint     NOT NULL CHECK (buy_day BETWEEN 1 AND 28),
    ticker               varchar(16)  NOT NULL,
    start_date           date         NOT NULL,
    timezone             varchar(64)  NOT NULL,
    created_at           timestamp    NOT NULL DEFAULT now(),
    updated_at           timestamp,
    deleted_at           timestamp,
    notes                text
);

COMMENT ON COLUMN invest_settings.buy_day IS 'Day of month the contribution is due; capped at 28 so every month has it';

INSERT INTO invest_settings (id, monthly_amount_cents, currency, buy_day, ticker, start_date, timezone)
VALUES (1, 30000, 'USD', 1, 'VT', CURRENT_DATE, 'UTC');

CREATE TABLE invest_contributions (
    id             bigserial      PRIMARY KEY,
    contributed_on date           NOT NULL,
    amount_cents   bigint         NOT NULL CHECK (amount_cents > 0),
    shares         numeric(20, 8) NOT NULL CHECK (shares > 0),
    price          numeric(14, 4) NOT NULL CHECK (price > 0),
    fee_cents      bigint         NOT NULL DEFAULT 0 CHECK (fee_cents >= 0),
    created_at     timestamp      NOT NULL DEFAULT now(),
    updated_at     timestamp,
    deleted_at     timestamp,
    notes          text
);

CREATE INDEX idx_invest_contributions_date ON invest_contributions (contributed_on);

COMMENT ON COLUMN invest_contributions.amount_cents IS 'Cash invested, excluding fee';
COMMENT ON COLUMN invest_contributions.price IS 'Per-share execution price in the settings currency';

-- Daily close cache filled by the price job; not user data, so no soft delete.
CREATE TABLE invest_prices (
    id         bigserial      PRIMARY KEY,
    ticker     varchar(16)    NOT NULL,
    price_date date           NOT NULL,
    close      numeric(14, 4) NOT NULL CHECK (close > 0),
    CONSTRAINT uq_invest_prices_ticker_date UNIQUE (ticker, price_date)
);

CREATE TABLE invest_tags (
    id         bigserial    PRIMARY KEY,
    name       varchar(64)  NOT NULL,
    created_at timestamp    NOT NULL DEFAULT now(),
    updated_at timestamp,
    deleted_at timestamp,
    notes      text
);

-- Tag names are unique case-insensitively among live tags (a deleted tag frees its name).
CREATE UNIQUE INDEX uq_invest_tags_name ON invest_tags (lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE invest_journal_entries (
    id              bigserial    PRIMARY KEY,
    entry_type      varchar(16)  NOT NULL,
    mood            smallint     CHECK (mood BETWEEN 1 AND 5),
    body            text         NOT NULL,
    contribution_id bigint       REFERENCES invest_contributions (id),
    created_at      timestamp    NOT NULL DEFAULT now(),
    updated_at      timestamp,
    deleted_at      timestamp,
    notes           text
);

CREATE INDEX idx_invest_journal_entries_created ON invest_journal_entries (created_at);
CREATE INDEX idx_invest_journal_entries_contribution ON invest_journal_entries (contribution_id);

COMMENT ON COLUMN invest_journal_entries.entry_type IS 'JournalEntryType: CHECKIN, QUICK, DECISION';
COMMENT ON COLUMN invest_journal_entries.mood IS '1 (very worried) to 5 (very calm); optional';
COMMENT ON COLUMN invest_journal_entries.body IS 'Markdown';

CREATE TABLE invest_journal_entry_tags (
    entry_id bigint NOT NULL REFERENCES invest_journal_entries (id) ON DELETE CASCADE,
    tag_id   bigint NOT NULL REFERENCES invest_tags (id),
    PRIMARY KEY (entry_id, tag_id)
);

CREATE INDEX idx_invest_journal_entry_tags_tag ON invest_journal_entry_tags (tag_id);

CREATE TABLE invest_rules (
    id         bigserial    PRIMARY KEY,
    text       varchar(500) NOT NULL,
    sort_order integer      NOT NULL DEFAULT 0,
    created_at timestamp    NOT NULL DEFAULT now(),
    updated_at timestamp,
    deleted_at timestamp,
    notes      text
);

CREATE TABLE invest_sources (
    id          bigserial    PRIMARY KEY,
    title       varchar(255) NOT NULL,
    source_type varchar(16)  NOT NULL,
    url         varchar(2048),
    status      varchar(16)  NOT NULL DEFAULT 'TO_DO',
    created_at  timestamp    NOT NULL DEFAULT now(),
    updated_at  timestamp,
    deleted_at  timestamp,
    notes       text
);

COMMENT ON COLUMN invest_sources.source_type IS 'SourceType: BOOK, VIDEO, ARTICLE, PODCAST, OTHER';
COMMENT ON COLUMN invest_sources.status IS 'SourceStatus: TO_DO, IN_PROGRESS, DONE';

CREATE TABLE invest_learning_notes (
    id         bigserial    PRIMARY KEY,
    title      varchar(255) NOT NULL,
    body       text         NOT NULL,
    source_id  bigint       REFERENCES invest_sources (id),
    created_at timestamp    NOT NULL DEFAULT now(),
    updated_at timestamp,
    deleted_at timestamp,
    notes      text
);

CREATE INDEX idx_invest_learning_notes_source ON invest_learning_notes (source_id);

CREATE TABLE invest_learning_note_tags (
    note_id bigint NOT NULL REFERENCES invest_learning_notes (id) ON DELETE CASCADE,
    tag_id  bigint NOT NULL REFERENCES invest_tags (id),
    PRIMARY KEY (note_id, tag_id)
);

CREATE INDEX idx_invest_learning_note_tags_tag ON invest_learning_note_tags (tag_id);

INSERT INTO invest_tags (name) VALUES ('emotion'), ('learning'), ('market-event'), ('decision');

INSERT INTO invest_rules (text, sort_order) VALUES
    ('Never sell in a crash.', 1),
    ('Never change the plan without writing the reason in the journal first.', 2),
    ('Wait 72 hours before any deviation.', 3);
