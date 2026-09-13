-- Lets the MT5 importer pull deposit/withdrawal balance operations
-- (MT5's DEAL_TYPE_BALANCE deals) directly from the broker and dedupe them
-- the same way trades already are: nullable, so existing manually-entered
-- capital rows (and every PROFIT/LOSS row TradeService.create() books
-- alongside a trade) are untouched and simply never set it.
ALTER TABLE capitals ADD COLUMN external_id varchar(64);

COMMENT ON COLUMN capitals.external_id IS 'Broker-side identifier (MT5 balance-operation deal ticket) used to dedupe re-imports; null for manually-entered or trade-derived (PROFIT/LOSS) rows';

-- Same partial-unique-index pattern as ux_trades_external_id (V2): only rows
-- that actually set external_id participate in the uniqueness check, so
-- re-running the importer over an overlapping date range is safe.
CREATE UNIQUE INDEX ux_capitals_external_id ON capitals (external_id) WHERE external_id IS NOT NULL;
