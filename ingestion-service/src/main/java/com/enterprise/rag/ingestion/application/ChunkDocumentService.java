package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.ingestion.config.IngestionProperties;
import com.enterprise.rag.ingestion.config.SagaAfterCommitPublisher;
import com.enterprise.rag.ingestion.domain.Chunker;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.TextChunk;
import com.enterprise.rag.ingestion.ports.out.ChunkRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// System design pattern: Saga — step 2: chunk (pure Chunker + persist)

@Service
@RequiredArgsConstructor
public class ChunkDocumentService {

  private final DocumentRepositoryPort documentRepository;
  private final ChunkRepositoryPort chunkRepository;
  private final SagaEventPublisherPort sagaEventPublisher;
  private final SagaAfterCommitPublisher afterCommitPublisher;
  private final IngestionFailureHandler failureHandler;
  private final IngestionProperties properties;

  @Transactional
  public Either<AppError, DocumentId> handle(DocumentParsedEvent event) {
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
                  return chunkAndPublish(documentId, tenantId, event, doc);
                });

    result.peekLeft(error -> failureHandler.fail(documentId, tenantId, "chunk", error));
    return result;
  }

  private Either<AppError, DocumentId> chunkAndPublish(
      DocumentId documentId, TenantId tenantId, DocumentParsedEvent event, Document document) {
    if (document.extractedText().isEmpty()) {
      if (document.status() == DocumentStatus.PENDING
          || document.status() == DocumentStatus.PROCESSING) {
        throw new TransientIngestionException(
            "extracted text not yet available for document " + documentId.asString());
      }
      return Either.left((AppError) new IngestionError("chunk", "missing extracted text", null));
    }
    String text = document.extractedText().get();
    List<TextChunk> chunks =
        Chunker.chunk(text, properties.chunking().size(), properties.chunking().overlap());
    return validateChunks(chunks)
        .flatMap(ignored -> saveChunksAndPublish(documentId, tenantId, event, chunks));
  }

  private Either<AppError, DocumentId> saveChunksAndPublish(
      DocumentId documentId, TenantId tenantId, DocumentParsedEvent event, List<TextChunk> chunks) {
    chunkRepository.saveAll(documentId, tenantId, chunks);
    DocumentChunkedEvent chunkedEvent =
        new DocumentChunkedEvent(
            OutboxMessageId.generate().asString(),
            event.documentId(),
            event.tenantId(),
            chunks.size());
    afterCommitPublisher.runAfterCommit(() -> sagaEventPublisher.publishChunked(chunkedEvent));
    return Either.right(documentId);
  }

  private Either<AppError, Boolean> validateChunks(List<TextChunk> chunks) {
    if (chunks.isEmpty()) {
      return Either.left((AppError) new IngestionError("chunk", "no chunks produced", null));
    }
    return Either.right(Boolean.TRUE);
  }
}
