-- Groundwork for AI search (Phase 9). Nothing writes to this table yet: it fixes the shape so a
-- later feature can embed journal entries and learning notes without another migration.
--
-- One row per (kind of thing, its id, embedding model). The vector is a plain real[] so no
-- PostgreSQL extension is needed; similarity can be computed in the application at personal scale,
-- or the column moved to pgvector later. content_hash lets a re-index skip text that has not changed.

CREATE TABLE invest_embeddings (
    id           bigserial    PRIMARY KEY,
    source_type  varchar(16)  NOT NULL,
    source_id    bigint       NOT NULL,
    model        varchar(100) NOT NULL,
    dimensions   integer      NOT NULL CHECK (dimensions BETWEEN 1 AND 8192),
    vector       real[]       NOT NULL,
    content_hash varchar(64)  NOT NULL,
    created_at   timestamp    NOT NULL DEFAULT now(),
    updated_at   timestamp,
    CONSTRAINT uq_invest_embeddings UNIQUE (source_type, source_id, model),
    CONSTRAINT ck_invest_embeddings_dims CHECK (array_length(vector, 1) = dimensions),
    CONSTRAINT ck_invest_embeddings_hash CHECK (length(content_hash) = 64)
);

CREATE INDEX idx_invest_embeddings_source ON invest_embeddings (source_type, source_id);

COMMENT ON COLUMN invest_embeddings.source_type IS 'EmbeddingSource: JOURNAL, NOTE';
COMMENT ON COLUMN invest_embeddings.content_hash IS 'SHA-256 hex of the text that was embedded';
