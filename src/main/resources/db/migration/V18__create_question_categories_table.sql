CREATE TABLE question_categories (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    notes TEXT,
    name VARCHAR(255) NOT NULL
);

ALTER TABLE question_answers
    ADD COLUMN category_id BIGINT;

ALTER TABLE question_answers
    ADD CONSTRAINT fk_question_answers_category
        FOREIGN KEY (category_id)
        REFERENCES question_categories(id);
