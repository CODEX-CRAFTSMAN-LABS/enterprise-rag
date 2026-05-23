package com.enterprise.rag.ingestion.adapters.in.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.ingestion.application.EmbedIndexDocumentService;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentId;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentChunkedConsumerTest {

  @Mock private EmbedIndexDocumentService embedIndexDocumentService;
  @Mock private SagaEventDeserializer deserializer;

  @InjectMocks private DocumentChunkedConsumer consumer;

  @Test
  void should_delegateToEmbedIndexService() {
    DocumentChunkedEvent event =
        new DocumentChunkedEvent("e1", DocumentId.generate().asString(), "acme", 3);
    when(deserializer.read(any(), any())).thenReturn(event);
    when(embedIndexDocumentService.handle(event))
        .thenReturn(Either.right(DocumentId.of(event.documentId())));

    consumer.onChunked("{\"documentId\":\"x\"}");

    verify(embedIndexDocumentService).handle(event);
  }
}
