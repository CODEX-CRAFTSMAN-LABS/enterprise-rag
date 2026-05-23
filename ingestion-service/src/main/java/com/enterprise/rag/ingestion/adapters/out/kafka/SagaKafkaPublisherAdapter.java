package com.enterprise.rag.ingestion.adapters.out.kafka;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.ingestion.config.IngestionProperties;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentIngestionFailedEvent;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import com.enterprise.rag.ingestion.ports.out.SagaEventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.saga.enabled", havingValue = "true")
class SagaKafkaPublisherAdapter implements SagaEventPublisherPort {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final IngestionProperties properties;
  private final ObjectMapper objectMapper;

  @Override
  public Either<AppError, Boolean> publishParsed(DocumentParsedEvent event) {
    return publish(properties.kafka().documentParsedTopic(), event.tenantId(), event);
  }

  @Override
  public Either<AppError, Boolean> publishChunked(DocumentChunkedEvent event) {
    return publish(properties.kafka().documentChunkedTopic(), event.tenantId(), event);
  }

  @Override
  public Either<AppError, Boolean> publishFailed(DocumentIngestionFailedEvent event) {
    return publish(properties.kafka().documentIngestionFailedTopic(), event.tenantId(), event);
  }

  private Either<AppError, Boolean> publish(String topic, String key, Object event) {
    return Try.of(() -> kafkaTemplate.send(topic, key, toJson(event)).get())
        .toEither()
        .mapLeft(t -> (AppError) new IngestionError("saga-publish", t.getMessage(), t))
        .map(ignored -> Boolean.TRUE);
  }

  private String toJson(Object event) throws JsonProcessingException {
    return objectMapper.writeValueAsString(event);
  }
}
