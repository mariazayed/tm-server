-- Several funds instead of one (general investing, step 1).
--
-- Until now the fund lived in invest_settings.ticker and a buy carried no ticker, so the tracker could only
-- describe a plan that buys one thing. This adds the funds you hold and ties every buy to one of them.
--
-- invest_settings.ticker stays, as the ticker of the first fund, so everything that still reads it keeps
-- working. Existing data is carried over: one fund is created from the current ticker and every existing
-- buy is attached to it.

CREATE TABLE invest_holdings (
    id            bigserial     PRIMARY KEY,
    ticker        varchar(16)   NOT NULL,
    name          varchar(120),
    asset_class   varchar(24)   NOT NULL DEFAULT 'EQUITY',
    region        varchar(32)   NOT NULL DEFAULT 'Global',
    target_pct    numeric(5, 2) NOT NULL DEFAULT 100 CHECK (target_pct >= 0 AND target_pct <= 100),
    expense_ratio numeric(5, 3) CHECK (expense_ratio >= 0 AND expense_ratio <= 5),
    created_at    timestamp     NOT NULL DEFAULT now(),
    updated_at    timestamp,
    deleted_at    timestamp,
    notes         text
);

-- One live fund per ticker, whatever the letter case.
CREATE UNIQUE INDEX uq_invest_holdings_ticker ON invest_holdings (upper(ticker)) WHERE deleted_at IS NULL;

COMMENT ON COLUMN invest_holdings.asset_class IS 'EQUITY, BONDS, CASH, REAL_ESTATE, COMMODITIES or OTHER';
COMMENT ON COLUMN invest_holdings.target_pct IS 'Share of the portfolio you aim for; the targets of live funds should add up to 100';
COMMENT ON COLUMN invest_holdings.expense_ratio IS 'Yearly fund charge in percent, e.g. 0.060';

INSERT INTO invest_holdings (ticker)
SELECT ticker FROM invest_settings WHERE id = 1;

ALTER TABLE invest_contributions ADD COLUMN holding_id bigint REFERENCES invest_holdings (id);

UPDATE invest_contributions
SET holding_id = (SELECT min(id) FROM invest_holdings WHERE deleted_at IS NULL);

ALTER TABLE invest_contributions ALTER COLUMN holding_id SET NOT NULL;

CREATE INDEX idx_invest_contributions_holding ON invest_contributions (holding_id);
