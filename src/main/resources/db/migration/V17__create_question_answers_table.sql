CREATE TABLE question_answers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    notes TEXT,
    question TEXT NOT NULL,
    answer TEXT NOT NULL
);
