package com.enterprise.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.common.error.ValidationError;
import com.enterprise.rag.ingestion.config.IngestionMetrics;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.UploadCommand;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.OutboxRepositoryPort;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadDocumentServiceTest {

  @Mock private DocumentRepositoryPort documentRepository;
  @Mock private OutboxRepositoryPort outboxRepository;
  @Mock private IngestionMetrics ingestionMetrics;

  private UploadDocumentService service;

  @BeforeEach
  void setUp() {
    service = new UploadDocumentService(documentRepository, outboxRepository, ingestionMetrics);
  }

  @Test
  void should_returnDocumentId_when_uploadIsValid() {
    UploadCommand command =
        new UploadCommand(
            new TenantId("tenant-a"), "policy.pdf", "application/pdf", "sample".getBytes());

    when(documentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Document doc = invocation.getArgument(0);
              return doc;
            });

    var result = service.upload(command);

    assertThat(result.isRight()).isTrue();
    assertThat(result.get().value()).isNotNull();

    ArgumentCaptor<DocumentUploadedEvent> eventCaptor =
        ArgumentCaptor.forClass(DocumentUploadedEvent.class);
    verify(outboxRepository).enqueue(eventCaptor.capture());
    assertThat(eventCaptor.getValue().tenantId()).isEqualTo("tenant-a");
    assertThat(eventCaptor.getValue().filename()).isEqualTo("policy.pdf");
  }

  @Test
  void should_returnValidationError_when_tenantIdInvalid() {
    UploadCommand command =
        new UploadCommand(new TenantId("x"), "", "application/pdf", new byte[0]);

    var result = service.upload(command);

    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isInstanceOf(ValidationError.class);
  }

  @Test
  void should_persistPendingDocument_when_valid() {
    UploadCommand command =
        new UploadCommand(new TenantId("acme-corp"), "notes.txt", "text/plain", "hello".getBytes());

    when(documentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var saved = service.upload(command).get();

    ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
    verify(documentRepository).save(docCaptor.capture());
    assertThat(docCaptor.getValue().status()).isEqualTo(DocumentStatus.PENDING);
    assertThat(docCaptor.getValue().id()).isEqualTo(saved);
    assertThat(docCaptor.getValue().createdAt()).isBeforeOrEqualTo(Instant.now());
  }
}
