package com.enterprise.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.ingestion.config.SagaAfterCommitPublisher;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import com.enterprise.rag.ingestion.ports.out.TextExtractorPort;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParseDocumentServiceTest {

  @Mock private DocumentRepositoryPort documentRepository;
  @Mock private TextExtractorPort textExtractor;
  @Mock private SagaEventPublisherPort sagaEventPublisher;
  @Mock private SagaAfterCommitPublisher afterCommitPublisher;
  @Mock private IngestionFailureHandler failureHandler;

  private ParseDocumentService service;
  private DocumentId documentId;
  private TenantId tenantId;

  @BeforeEach
  void setUp() {
    service =
        new ParseDocumentService(
            documentRepository,
            textExtractor,
            sagaEventPublisher,
            afterCommitPublisher,
            failureHandler);
    documentId = DocumentId.generate();
    tenantId = new TenantId("acme-corp");
  }

  @Test
  void should_returnNotFound_when_documentMissing() {
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.none());

    var event = uploadedEvent();
    var result = service.handle(event);

    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isInstanceOf(NotFoundError.class);
    verify(failureHandler).fail(eq(documentId), eq(tenantId), eq("parse"), any());
  }

  @Test
  void should_skipParse_when_alreadyIndexed() {
    Document indexed =
        TestDocuments.document(documentId, tenantId, DocumentStatus.INDEXED, Option.none());
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(indexed));

    var result = service.handle(uploadedEvent());

    assertThat(result.isRight()).isTrue();
    verify(textExtractor, never()).extract(any());
    verify(failureHandler, never()).fail(any(), any(), any(), any());
  }

  @Test
  void should_skipParse_when_processingWithExtractedText() {
    Document processing =
        TestDocuments.document(
            documentId, tenantId, DocumentStatus.PROCESSING, Option.of("already parsed"));
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(processing));

    var result = service.handle(uploadedEvent());

    assertThat(result.isRight()).isTrue();
    verify(textExtractor, never()).extract(any());
  }

  @Test
  void should_extractAndPublish_when_pending() {
    Document pending =
        TestDocuments.document(documentId, tenantId, DocumentStatus.PENDING, Option.none());
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(pending));
    when(textExtractor.extract(pending)).thenReturn(Either.right("parsed text content"));
    doAnswer(
            inv -> {
              inv.getArgument(0, Runnable.class).run();
              return null;
            })
        .when(afterCommitPublisher)
        .runAfterCommit(any());

    var result = service.handle(uploadedEvent());

    assertThat(result.isRight()).isTrue();
    verify(documentRepository).updateExtractedText(documentId, tenantId, "parsed text content");

    ArgumentCaptor<DocumentParsedEvent> parsed = ArgumentCaptor.forClass(DocumentParsedEvent.class);
    verify(sagaEventPublisher).publishParsed(parsed.capture());
    assertThat(parsed.getValue().documentId()).isEqualTo(documentId.asString());
    assertThat(parsed.getValue().textLength()).isEqualTo("parsed text content".length());
  }

  private DocumentUploadedEvent uploadedEvent() {
    return new DocumentUploadedEvent(
        "evt-1", documentId.asString(), tenantId.value(), "notes.txt", "text/plain", 5);
  }
}
