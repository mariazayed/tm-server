-- V11 collapsed the original VELUCRA_AI/BOOK_WRITING/CONTENT categories into
-- a single LEARNING_OTHERS bucket. Turns out Learning and Others aren't the
-- same thing and should stay separate categories -- splitting them back out
-- here (V11 already ran against the live DB, so it can't be edited; this is
-- a new migration on top of it, same reasoning as V11 vs V10).
--
-- The original per-row category is recoverable from V10's seed data (still
-- readable there even though the live rows were already overwritten by
-- V11): decision_number 2 and 3 were BOOK_WRITING (the two books) --
-- these become LEARNING. decision_number 1 (founding the company) and 4
-- (the Instagram page) were VELUCRA_AI/CONTENT -- these become OTHERS.
-- Any other row that ended up in LEARNING_OTHERS by some other path (e.g.
-- created after V11, through the app) defaults to OTHERS as a safe
-- catch-all -- review those manually via the category dropdown if any
-- should actually be Learning.

UPDATE decisions SET category = 'OTHERS' WHERE category = 'LEARNING_OTHERS';
UPDATE decisions SET category = 'LEARNING' WHERE decision_number IN (2, 3);

COMMENT ON COLUMN decisions.category IS 'DecisionCategoryEnum: TRADING, INVESTING, PERSONAL, SYSTEM, LEARNING, OTHERS';
