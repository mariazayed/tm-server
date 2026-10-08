-- Full-text search over the journal and learning notes (AI assistant, step 2). Used by the chat to find
-- the entries that relate to a question; it replaces nothing and stores no new owner data.
--
-- The vectors are generated columns, so they can never go stale: PostgreSQL recomputes them whenever
-- body (or title) changes, and the application never writes them. The 'english' configuration stems
-- words (worried / worry) and ignores common words. Soft-deleted rows stay indexed but are filtered by
-- the query (deleted_at IS NULL), the same way every other read does it.

ALTER TABLE invest_journal_entries
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('english', body)) STORED;

CREATE INDEX idx_invest_journal_entries_search ON invest_journal_entries USING gin (search_vector);

-- A word in the title counts for more than the same word in the body (weights A and B).
ALTER TABLE invest_learning_notes
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A')
        || setweight(to_tsvector('english', coalesce(body, '')), 'B')
    ) STORED;

CREATE INDEX idx_invest_learning_notes_search ON invest_learning_notes USING gin (search_vector);
