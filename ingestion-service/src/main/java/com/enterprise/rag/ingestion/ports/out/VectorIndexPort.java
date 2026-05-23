package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.TextChunk;
import io.vavr.collection.List;
import io.vavr.control.Either;

public interface VectorIndexPort {

  Either<AppError, Integer> index(DocumentId documentId, TenantId tenantId, List<TextChunk> chunks);
}
