package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.config.SagaAfterCommitPublisher;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import com.enterprise.rag.ingestion.ports.out.TextExtractorPort;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// System design pattern: Saga (choreography) — step 1: parse
// Idempotent: only processes PENDING documents.

@Service
@RequiredArgsConstructor
public class ParseDocumentService {

  private final DocumentRepositoryPort documentRepository;
  private final TextExtractorPort textExtractor;
  private final SagaEventPublisherPort sagaEventPublisher;
  private final SagaAfterCommitPublisher afterCommitPublisher;
  private final IngestionFailureHandler failureHandler;

  @Transactional
  public Either<AppError, DocumentId> handle(DocumentUploadedEvent event) {
    DocumentId documentId = DocumentId.of(event.documentId());
    TenantId tenantId = new TenantId(event.tenantId());

    Either<AppError, DocumentId> result =
        documentRepository
            .findById(documentId, tenantId)
            .toEither(() -> (AppError) new NotFoundError("Document", event.documentId()))
            .flatMap(
                doc -> {
                  if (doc.status() == DocumentStatus.INDEXED || doc.status() == DocumentStatus.FAILED) {
                    return Either.right(documentId);
                  }
                  if (doc.status() == DocumentStatus.PROCESSING && doc.extractedText().isDefined()) {
                    return Either.right(documentId);
                  }
                  return parseAndPublish(documentId, tenantId, event, doc);
                });

    result.peekLeft(error -> failureHandler.fail(documentId, tenantId, "parse", error));
    return result;
  }

  private Either<AppError, DocumentId> parseAndPublish(
      DocumentId documentId, TenantId tenantId, DocumentUploadedEvent event, Document document) {
    return textExtractor
        .extract(document)
        .flatMap(
            text -> {
              documentRepository.updateExtractedText(documentId, tenantId, text);
              DocumentParsedEvent parsedEvent =
                  new DocumentParsedEvent(
                      OutboxMessageId.generate().asString(),
                      event.documentId(),
                      event.tenantId(),
                      text.length());
              afterCommitPublisher.runAfterCommit(
                  () -> sagaEventPublisher.publishParsed(parsedEvent));
              return Either.<AppError, DocumentId>right(documentId);
            });
  }
}
