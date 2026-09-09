-- Phase 5: JSON-encoded structure events (swing breaks/BOS/CHoCH, order
-- blocks, FVGs, liquidity sweeps) detected by the MT5 importer's
-- deterministic structure_detection.py. Free-form text, not a relational
-- shape, since this is read by AiSuggestionService's prompt builder, not
-- queried against -- keeping it simple avoids a JSONB/array mapping
-- decision the way TradeInsight.detectedMistakeTags already sidestepped
-- one in Phase 2.
ALTER TABLE trades ADD COLUMN detected_structures text;
