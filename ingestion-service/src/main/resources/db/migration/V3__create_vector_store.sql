-- Spring AI pgvector read/write table (768 dims = nomic-embed-text via Ollama)
-- Kept in Flyway so schema exists without spring.ai.vectorstore.pgvector.initialize-schema=true

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content    TEXT,
    metadata   JSONB,
    embedding  vector(768)
);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding_hnsw
    ON vector_store USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_tenant
    ON vector_store ((metadata ->> 'tenantId'));
