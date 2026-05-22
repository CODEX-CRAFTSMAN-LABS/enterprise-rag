package com.enterprise.rag.ingestion.adapters.in.kafka;

import com.enterprise.rag.ingestion.application.ParseDocumentService;
import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.saga.enabled", havingValue = "true")
public class DocumentUploadedConsumer {

  private final ParseDocumentService parseDocumentService;
  private final SagaEventDeserializer deserializer;

  @KafkaListener(
      topics = "${ingestion.kafka.document-uploaded-topic}",
      groupId = "${spring.kafka.consumer.group-id}-uploaded")
  public void onUploaded(String payload) {
    DocumentUploadedEvent event = deserializer.read(payload, DocumentUploadedEvent.class);
    log.info("Saga step parse: documentId={}", event.documentId());
    parseDocumentService.handle(event);
  }
}
