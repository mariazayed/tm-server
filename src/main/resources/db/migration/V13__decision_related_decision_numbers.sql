-- Allow a decision to reconsider several earlier decisions instead of only
-- one. Preserve existing links by wrapping the old scalar value in an array.

ALTER TABLE decisions ADD COLUMN related_decision_numbers integer[] NOT NULL DEFAULT '{}';

UPDATE decisions
SET related_decision_numbers = ARRAY[related_decision_number]
WHERE related_decision_number IS NOT NULL;

ALTER TABLE decisions DROP COLUMN related_decision_number;

COMMENT ON COLUMN decisions.related_decision_numbers IS 'decision_numbers of earlier decisions this decision reconsiders/revises/reverses';
