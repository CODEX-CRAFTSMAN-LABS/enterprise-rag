package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.common.error.ValidationError;
import com.enterprise.rag.ingestion.config.IngestionMetrics;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.domain.UploadCommand;
import com.enterprise.rag.ingestion.domain.UploadDocumentValidator;
import com.enterprise.rag.ingestion.ports.in.UploadDocumentUseCase;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.OutboxRepositoryPort;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// System design pattern: Outbox + CQRS write model
// Persists document and outbox row in one transaction; Kafka publish happens asynchronously
// via OutboxPoller so we never dual-write to DB and broker.

@Service
@RequiredArgsConstructor
public class UploadDocumentService implements UploadDocumentUseCase {

  private final DocumentRepositoryPort documentRepository;
  private final OutboxRepositoryPort outboxRepository;
  private final IngestionMetrics ingestionMetrics;

  @Override
  @Transactional
  public Either<AppError, DocumentId> upload(UploadCommand command) {
    return UploadDocumentValidator.validate(command)
        .toEither()
        .mapLeft(errors -> (AppError) new ValidationError(errors))
        .flatMap(this::persistDocumentAndOutbox);
  }

  private Either<AppError, DocumentId> persistDocumentAndOutbox(UploadCommand command) {
    return Try.of(() -> save(command))
        .toEither()
        .mapLeft(t -> (AppError) new IngestionError("upload", t.getMessage(), t))
        .map(Document::id)
        .peek(id -> ingestionMetrics.recordUpload(command.tenantId().value()));
  }

  private Document save(UploadCommand command) {
    Instant now = Instant.now();
    DocumentId documentId = DocumentId.generate();
    Document document =
        new Document(
            documentId,
            command.tenantId(),
            command.filename(),
            command.contentType(),
            command.content().length,
            DocumentStatus.PENDING,
            command.content(),
            Option.none(),
            now,
            now);

    Document saved = documentRepository.save(document);
    String eventId = OutboxMessageId.generate().asString();
    DocumentUploadedEvent event = DocumentUploadedEvent.from(saved, eventId);
    outboxRepository.enqueue(event);
    return saved;
  }
}
