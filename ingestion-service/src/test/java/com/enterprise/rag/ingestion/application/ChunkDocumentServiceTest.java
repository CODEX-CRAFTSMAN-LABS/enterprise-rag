package com.enterprise.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.ingestion.config.IngestionProperties;
import com.enterprise.rag.ingestion.config.SagaAfterCommitPublisher;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.ChunkRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import io.vavr.control.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChunkDocumentServiceTest {

  @Mock private DocumentRepositoryPort documentRepository;
  @Mock private ChunkRepositoryPort chunkRepository;
  @Mock private SagaEventPublisherPort sagaEventPublisher;
  @Mock private SagaAfterCommitPublisher afterCommitPublisher;
  @Mock private IngestionFailureHandler failureHandler;

  private ChunkDocumentService service;
  private DocumentId documentId;
  private TenantId tenantId;
  private IngestionProperties properties;

  @BeforeEach
  void setUp() {
    properties =
        new IngestionProperties(
            new IngestionProperties.Outbox(true, 10, java.time.Duration.ofSeconds(1)),
            new IngestionProperties.Saga(true),
            new IngestionProperties.Chunking(50, 10),
            new IngestionProperties.Kafka("u", "p", "c", "f"));
    service =
        new ChunkDocumentService(
            documentRepository,
            chunkRepository,
            sagaEventPublisher,
            afterCommitPublisher,
            failureHandler,
            properties);
    documentId = DocumentId.generate();
    tenantId = new TenantId("acme-corp");
  }

  @Test
  void should_returnNotFound_when_documentMissing() {
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.none());

    var result = service.handle(parsedEvent());

    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isInstanceOf(NotFoundError.class);
    verify(failureHandler).fail(eq(documentId), eq(tenantId), eq("chunk"), any());
  }

  @Test
  void should_skipChunk_when_alreadyIndexed() {
    Document indexed =
        TestDocuments.document(documentId, tenantId, DocumentStatus.INDEXED, Option.of("text"));
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(indexed));

    var result = service.handle(parsedEvent());

    assertThat(result.isRight()).isTrue();
    verify(chunkRepository, never()).saveAll(any(), any(), any());
  }

  @Test
  void should_throwTransient_when_textNotReadyYet() {
    Document pending =
        TestDocuments.document(documentId, tenantId, DocumentStatus.PENDING, Option.none());
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(pending));

    assertThatThrownBy(() -> service.handle(parsedEvent()))
        .isInstanceOf(TransientIngestionException.class);
  }

  @Test
  void should_returnIngestionError_when_whitespaceOnlyText() {
    Document failed =
        TestDocuments.document(documentId, tenantId, DocumentStatus.FAILED, Option.of("   "));
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(failed));

    var result = service.handle(parsedEvent());

    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isInstanceOf(IngestionError.class);
  }

  @Test
  void should_chunkAndPublish_when_textPresent() {
    String text = "This is a sample document body that will be split into multiple chunks.";
    Document processing =
        TestDocuments.document(documentId, tenantId, DocumentStatus.PROCESSING, Option.of(text));
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(processing));
    doAnswer(
            inv -> {
              inv.getArgument(0, Runnable.class).run();
              return null;
            })
        .when(afterCommitPublisher)
        .runAfterCommit(any());

    var result = service.handle(parsedEvent());

    assertThat(result.isRight()).isTrue();
    verify(chunkRepository).saveAll(eq(documentId), eq(tenantId), any());

    ArgumentCaptor<DocumentChunkedEvent> chunked =
        ArgumentCaptor.forClass(DocumentChunkedEvent.class);
    verify(sagaEventPublisher).publishChunked(chunked.capture());
    assertThat(chunked.getValue().chunkCount()).isPositive();
  }

  private DocumentParsedEvent parsedEvent() {
    return new DocumentParsedEvent("evt-2", documentId.asString(), tenantId.value(), 100);
  }
}
