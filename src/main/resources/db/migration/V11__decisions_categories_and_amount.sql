-- Decisions feature adjustments, same day as V10:
-- 1) Categories collapsed from 7 to 5: VELUCRA_AI, BOOK_WRITING and CONTENT
--    are folded into a new LEARNING_OTHERS bucket; TRADING, INVESTING,
--    PERSONAL and SYSTEM are kept as-is.
-- 2) `amount` column removed -- not used enough to justify a dedicated
--    field; any $ figures that matter stay in description/reasoning text.
-- 3) `decision_number` is now server-assigned (DecisionService.create),
--    no schema change needed for that -- existing values (1-11 from V10's
--    backfill) are left untouched and the sequence continues from there.

-- Remap old category values to their new home BEFORE the Java enum stops
-- recognizing them -- Hibernate would fail to read the row otherwise
-- (category is stored as the enum name via @Enumerated(EnumType.STRING)).
UPDATE decisions SET category = 'LEARNING_OTHERS' WHERE category IN ('VELUCRA_AI', 'BOOK_WRITING', 'CONTENT');

ALTER TABLE decisions DROP COLUMN amount;

COMMENT ON COLUMN decisions.category IS 'DecisionCategoryEnum: TRADING, INVESTING, PERSONAL, SYSTEM, LEARNING_OTHERS';
