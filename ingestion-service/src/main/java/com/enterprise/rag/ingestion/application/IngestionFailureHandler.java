package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentIngestionFailedEvent;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// System design pattern: Saga compensation — mark FAILED and emit failure event.

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionFailureHandler {

  private final DocumentRepositoryPort documentRepository;
  private final SagaEventPublisherPort sagaEventPublisher;

  public void fail(DocumentId documentId, TenantId tenantId, String step, AppError error) {
    log.warn(
        "Ingestion failed document={} step={} reason={}",
        documentId.asString(),
        step,
        error.message());
    documentRepository.updateStatus(documentId, tenantId, DocumentStatus.FAILED);
    sagaEventPublisher.publishFailed(
        new DocumentIngestionFailedEvent(
            OutboxMessageId.generate().asString(),
            documentId.asString(),
            tenantId.value(),
            step,
            error.message()));
  }
}
