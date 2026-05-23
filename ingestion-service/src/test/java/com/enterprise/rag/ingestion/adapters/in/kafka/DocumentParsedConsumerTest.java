package com.enterprise.rag.ingestion.adapters.in.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.ingestion.application.ChunkDocumentService;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentParsedConsumerTest {

  @Mock private ChunkDocumentService chunkDocumentService;
  @Mock private SagaEventDeserializer deserializer;

  @InjectMocks private DocumentParsedConsumer consumer;

  @Test
  void should_delegateToChunkService() {
    DocumentParsedEvent event =
        new DocumentParsedEvent("e1", DocumentId.generate().asString(), "acme", 42);
    when(deserializer.read(any(), any())).thenReturn(event);
    when(chunkDocumentService.handle(event))
        .thenReturn(Either.right(DocumentId.of(event.documentId())));

    consumer.onParsed("{\"documentId\":\"x\"}");

    verify(chunkDocumentService).handle(event);
  }
}
