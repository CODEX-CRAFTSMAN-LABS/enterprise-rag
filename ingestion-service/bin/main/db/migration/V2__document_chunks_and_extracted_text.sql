ALTER TABLE documents
    ADD COLUMN extracted_text TEXT;

CREATE TABLE document_chunks (
    id           UUID PRIMARY KEY,
    document_id  UUID         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    tenant_id    VARCHAR(64)  NOT NULL,
    chunk_index  INT          NOT NULL,
    content      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_chunks_document ON document_chunks (document_id);
CREATE INDEX idx_chunks_tenant ON document_chunks (tenant_id);
