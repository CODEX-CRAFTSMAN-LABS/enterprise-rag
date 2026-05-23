package com.enterprise.rag.ingestion.adapters.out.vector;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.TextChunk;
import com.enterprise.rag.ingestion.ports.out.VectorIndexPort;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.saga.enabled", havingValue = "true")
class VectorIndexAdapter implements VectorIndexPort {

  private final VectorStore vectorStore;

  @Override
  public Either<AppError, Integer> index(
      DocumentId documentId, TenantId tenantId, List<TextChunk> chunks) {
    return Try.run(() -> vectorStore.add(toAiDocuments(documentId, tenantId, chunks)))
        .toEither()
        .mapLeft(t -> (AppError) new IngestionError("embed-index", t.getMessage(), t))
        .map(ignored -> chunks.size());
  }

  private java.util.List<Document> toAiDocuments(
      DocumentId documentId, TenantId tenantId, List<TextChunk> chunks) {
    return chunks
        .map(
            chunk ->
                new Document(
                    UUID.randomUUID().toString(),
                    chunk.content(),
                    Map.of(
                        "tenantId",
                        tenantId.value(),
                        "documentId",
                        documentId.asString(),
                        "chunkIndex",
                        chunk.index())))
        .toJavaList();
  }
}
