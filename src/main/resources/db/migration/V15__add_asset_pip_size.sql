-- Powers the pip figure on manually-entered journal trades. Previously the
-- app hardcoded XAUUSD's pip size (0.1) for every asset and silently zeroed
-- pips out for anything else -- this lets each asset define its own.
-- Nullable and left unset until a person fills it in per asset (price
-- distance that equals 1 pip -- e.g. 0.0001 for a 4-decimal forex pair, 0.01
-- for a 2-decimal JPY pair, 0.1 for XAUUSD); pip/profit calculations are
-- simply skipped for any asset without it rather than guessing.
ALTER TABLE assets ADD COLUMN pip_size double precision;
