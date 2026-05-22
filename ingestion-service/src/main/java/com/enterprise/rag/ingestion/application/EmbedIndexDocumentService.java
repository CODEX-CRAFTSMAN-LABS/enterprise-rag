package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.ChunkRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.VectorIndexPort;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// System design pattern: Saga — step 3: embed + index into pgvector

@Service
@RequiredArgsConstructor
public class EmbedIndexDocumentService {

  private final DocumentRepositoryPort documentRepository;
  private final ChunkRepositoryPort chunkRepository;
  private final VectorIndexPort vectorIndexPort;
  private final IngestionFailureHandler failureHandler;

  @Transactional
  public Either<AppError, DocumentId> handle(DocumentChunkedEvent event) {
    DocumentId documentId = DocumentId.of(event.documentId());
    TenantId tenantId = new TenantId(event.tenantId());

    Either<AppError, DocumentId> result =
        documentRepository
            .findById(documentId, tenantId)
            .toEither(() -> (AppError) new NotFoundError("Document", event.documentId()))
            .flatMap(
                doc -> {
                  if (doc.status() == DocumentStatus.INDEXED) {
                    return Either.right(documentId);
                  }
                  return vectorIndexPort
                      .index(
                          documentId,
                          tenantId,
                          chunkRepository.findByDocument(documentId, tenantId))
                      .flatMap(
                          count -> {
                            documentRepository.updateStatus(
                                documentId, tenantId, DocumentStatus.INDEXED);
                            return Either.right(documentId);
                          });
                });

    result.peekLeft(error -> failureHandler.fail(documentId, tenantId, "embed-index", error));
    return result;
  }
}
