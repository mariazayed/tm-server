-- Flashcards for spaced-repetition review of learning notes (SM-2 scheduling).
-- Same conventions as V22: invest_ prefix, BaseEntity columns (created_at, updated_at,
-- deleted_at soft delete, notes). due_on is a plain date: reviews are scheduled per day, in the
-- plan's timezone, never to the minute.

CREATE TABLE invest_flashcards (
    id               bigserial     PRIMARY KEY,
    front            text          NOT NULL,
    back             text          NOT NULL,
    note_id          bigint        REFERENCES invest_learning_notes (id),
    due_on           date          NOT NULL,
    interval_days    integer       NOT NULL DEFAULT 0 CHECK (interval_days >= 0),
    ease             numeric(4, 2) NOT NULL DEFAULT 2.50 CHECK (ease >= 1.30),
    repetitions      integer       NOT NULL DEFAULT 0 CHECK (repetitions >= 0),
    lapses           integer       NOT NULL DEFAULT 0 CHECK (lapses >= 0),
    last_reviewed_on date,
    created_at       timestamp     NOT NULL DEFAULT now(),
    updated_at       timestamp,
    deleted_at       timestamp,
    notes            text
);

CREATE INDEX idx_invest_flashcards_due ON invest_flashcards (due_on) WHERE deleted_at IS NULL;
CREATE INDEX idx_invest_flashcards_note ON invest_flashcards (note_id);

COMMENT ON COLUMN invest_flashcards.ease IS 'SM-2 easiness factor, never below 1.30';
COMMENT ON COLUMN invest_flashcards.repetitions IS 'Consecutive successful reviews; reset to 0 by a failed one';
COMMENT ON COLUMN invest_flashcards.lapses IS 'Times a previously learned card was failed';
