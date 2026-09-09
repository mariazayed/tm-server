-- Phase 3: powers TradeEnrichmentService's risk-in-currency calculations.
-- Nullable and left unset until a person fills it in per asset (units per 1.0
-- lot -- e.g. 100000 for a standard forex lot); enrichment simply skips the
-- risk fields for any asset without it rather than guessing.
ALTER TABLE assets ADD COLUMN contract_size double precision;
