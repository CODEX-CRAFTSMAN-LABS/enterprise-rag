package com.enterprise.rag.ingestion.adapters.out.kafka;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.ingestion.config.IngestionProperties;
import com.enterprise.rag.ingestion.domain.OutboxMessage;
import com.enterprise.rag.ingestion.ports.out.EventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.outbox.enabled", havingValue = "true")
class KafkaEventPublisherAdapter implements EventPublisherPort {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final IngestionProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public Either<AppError, Boolean> publish(OutboxMessage message) {
    String topic = properties.kafka().documentUploadedTopic();
    String key = extractPartitionKey(message.payloadJson());
    return Try.run(() -> kafkaTemplate.send(topic, key, message.payloadJson()).get())
        .toEither()
        .mapLeft(t -> (AppError) new IngestionError("kafka-publish", t.getMessage(), t))
        .map(ignored -> Boolean.TRUE);
  }

  private String extractPartitionKey(String payloadJson) {
    return Try.of(() -> objectMapper.readTree(payloadJson))
        .map(node -> node.path("tenantId").asText("unknown"))
        .getOrElse("unknown");
  }
}
