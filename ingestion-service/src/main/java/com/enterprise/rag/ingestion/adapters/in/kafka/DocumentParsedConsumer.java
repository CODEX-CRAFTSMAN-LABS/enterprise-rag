package com.enterprise.rag.ingestion.adapters.in.kafka;

import com.enterprise.rag.ingestion.application.ChunkDocumentService;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.saga.enabled", havingValue = "true")
public class DocumentParsedConsumer {

  private final ChunkDocumentService chunkDocumentService;
  private final SagaEventDeserializer deserializer;

  @KafkaListener(
      topics = "${ingestion.kafka.document-parsed-topic}",
      groupId = "${spring.kafka.consumer.group-id}-parsed")
  public void onParsed(String payload) {
    DocumentParsedEvent event = deserializer.read(payload, DocumentParsedEvent.class);
    log.info("Saga step chunk: documentId={}", event.documentId());
    chunkDocumentService.handle(event);
  }
}
