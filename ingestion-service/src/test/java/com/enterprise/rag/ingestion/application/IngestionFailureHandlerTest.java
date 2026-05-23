package com.enterprise.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentIngestionFailedEvent;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestionFailureHandlerTest {

  @Mock private DocumentRepositoryPort documentRepository;
  @Mock private SagaEventPublisherPort sagaEventPublisher;

  @InjectMocks private IngestionFailureHandler handler;

  @Test
  void should_markFailedAndPublishEvent() {
    DocumentId documentId = DocumentId.generate();
    TenantId tenantId = new TenantId("acme-corp");
    var error = new IngestionError("parse", "extract failed", null);

    handler.fail(documentId, tenantId, "parse", error);

    verify(documentRepository).updateStatus(documentId, tenantId, DocumentStatus.FAILED);

    ArgumentCaptor<DocumentIngestionFailedEvent> event =
        ArgumentCaptor.forClass(DocumentIngestionFailedEvent.class);
    verify(sagaEventPublisher).publishFailed(event.capture());
    assertThat(event.getValue().step()).isEqualTo("parse");
    assertThat(event.getValue().reason()).contains("extract failed");
  }
}
