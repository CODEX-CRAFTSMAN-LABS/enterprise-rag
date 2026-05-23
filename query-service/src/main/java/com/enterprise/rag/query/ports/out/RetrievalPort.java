package com.enterprise.rag.query.ports.out;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.query.domain.RetrievedChunk;
import com.enterprise.rag.query.domain.TenantId;
import io.vavr.collection.List;
import io.vavr.control.Either;

public interface RetrievalPort {

  Either<AppError, List<RetrievedChunk>> retrieve(
      TenantId tenantId, float[] queryEmbedding, int topK);
}
