CREATE TABLE reminder_note_categories (
    id         bigserial PRIMARY KEY,
    name       varchar(255) NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp,
    deleted_at timestamp,
    notes      text
);

CREATE UNIQUE INDEX uq_reminder_note_categories_active_name
    ON reminder_note_categories (lower(name))
    WHERE deleted_at IS NULL;

ALTER TABLE reminder_notes
    ADD COLUMN category_id bigint REFERENCES reminder_note_categories(id);

CREATE INDEX idx_reminder_notes_category_id ON reminder_notes(category_id);
