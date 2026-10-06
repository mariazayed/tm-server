-- Development sample data for the investing tracker. NOT a Flyway migration (lives outside
-- db/migration) -- load it by hand into a local database:
--   psql -d tm_db -f src/main/resources/db/dev/invest-seed.sql
-- Safe to re-run: it clears the sample tables first. Never run against real data.

BEGIN;

TRUNCATE invest_learning_note_tags, invest_journal_entry_tags, invest_learning_notes,
         invest_sources, invest_journal_entries, invest_contributions, invest_prices
         RESTART IDENTITY CASCADE;

UPDATE invest_settings SET start_date = date_trunc('month', CURRENT_DATE) - interval '5 months' WHERE id = 1;

-- Six monthly buys of $300 (price is illustrative, not real market data).
INSERT INTO invest_contributions (contributed_on, amount_cents, shares, price, fee_cents)
SELECT d::date,
       30000,
       round(300.0 / p, 8),
       p,
       0
FROM (VALUES
    (date_trunc('month', CURRENT_DATE) - interval '5 months', 112.4000),
    (date_trunc('month', CURRENT_DATE) - interval '4 months', 114.1500),
    (date_trunc('month', CURRENT_DATE) - interval '3 months', 109.8000),
    (date_trunc('month', CURRENT_DATE) - interval '2 months', 111.2500),
    (date_trunc('month', CURRENT_DATE) - interval '1 month',  115.9000),
    (date_trunc('month', CURRENT_DATE),                       117.3000)
) AS t(d, p);

INSERT INTO invest_prices (ticker, price_date, close)
SELECT 'VT', contributed_on, price FROM invest_contributions;

INSERT INTO invest_journal_entries (entry_type, mood, body, contribution_id)
VALUES ('CHECKIN', 4, 'Bought on schedule. Market dipped a little; plan unchanged.', 3),
       ('QUICK', 2, 'Headlines are loud today. Not touching anything.', NULL),
       ('DECISION', 3, 'Considered adding a bond fund. Decided to wait 72 hours per the rules.', NULL);

INSERT INTO invest_journal_entry_tags (entry_id, tag_id)
SELECT e.id, t.id FROM invest_journal_entries e
JOIN invest_tags t ON (e.entry_type = 'CHECKIN' AND t.name = 'learning')
                   OR (e.entry_type = 'QUICK' AND t.name = 'emotion')
                   OR (e.entry_type = 'DECISION' AND t.name = 'decision');

INSERT INTO invest_sources (title, source_type, status)
VALUES ('The Simple Path to Wealth', 'BOOK', 'IN_PROGRESS'),
       ('Why diversification works', 'ARTICLE', 'DONE');

INSERT INTO invest_learning_notes (title, body, source_id)
VALUES ('Dollar-cost averaging', 'Investing a fixed amount on a schedule removes the need to time the market.', 1),
       ('Expense ratio', 'Fees compound too: a low-cost total-world ETF keeps more of the return.', 2);

INSERT INTO invest_learning_note_tags (note_id, tag_id)
SELECT n.id, t.id FROM invest_learning_notes n CROSS JOIN invest_tags t WHERE t.name = 'learning';

COMMIT;
