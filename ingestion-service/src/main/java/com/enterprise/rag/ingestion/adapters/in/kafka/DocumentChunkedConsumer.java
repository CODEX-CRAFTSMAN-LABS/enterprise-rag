package com.enterprise.rag.ingestion.adapters.in.kafka;

import com.enterprise.rag.ingestion.application.EmbedIndexDocumentService;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.saga.enabled", havingValue = "true")
public class DocumentChunkedConsumer {

  private final EmbedIndexDocumentService embedIndexDocumentService;
  private final SagaEventDeserializer deserializer;

  @KafkaListener(
      topics = "${ingestion.kafka.document-chunked-topic}",
      groupId = "${spring.kafka.consumer.group-id}-chunked")
  public void onChunked(String payload) {
    DocumentChunkedEvent event = deserializer.read(payload, DocumentChunkedEvent.class);
    log.info("Saga step embed+index: documentId={}", event.documentId());
    embedIndexDocumentService.handle(event);
  }
}
