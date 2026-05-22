package com.enterprise.rag.common.error;

/**
 * Root error type for the platform. All service failures map to a sealed subtype so {@code switch}
 * exhaustiveness checks compile and HTTP mapping stays consistent.
 */
public sealed interface AppError
    permits CacheMiss,
        EmbeddingError,
        IngestionError,
        LlmError,
        NotFoundError,
        RateLimitError,
        RetrievalError,
        TenantError,
        ValidationError {

  String message();
}
