package com.enterprise.rag.ingestion.adapters.in.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.ingestion.application.ParseDocumentService;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentUploadedConsumerTest {

  @Mock private ParseDocumentService parseDocumentService;
  @Mock private SagaEventDeserializer deserializer;

  @InjectMocks private DocumentUploadedConsumer consumer;

  @Test
  void should_delegateToParseService() {
    DocumentUploadedEvent event =
        new DocumentUploadedEvent(
            "e1", DocumentId.generate().asString(), "acme", "f.txt", "text/plain", 1);
    when(deserializer.read(any(), any())).thenReturn(event);
    when(parseDocumentService.handle(event))
        .thenReturn(Either.right(DocumentId.of(event.documentId())));

    consumer.onUploaded("{\"documentId\":\"x\"}");

    verify(parseDocumentService).handle(event);
  }
}
