package com.enterprise.rag.ingestion.adapters.out.db;

import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.enterprise.rag.ingestion.domain.OutboxMessage;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.ports.out.OutboxRepositoryPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.List;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OutboxRepositoryAdapter implements OutboxRepositoryPort {

  private final OutboxJpaRepository repository;
  private final ObjectMapper objectMapper;

  @Override
  public void enqueue(DocumentUploadedEvent event) {
    OutboxEventEntity entity = new OutboxEventEntity();
    entity.setId(OutboxMessageId.generate().value());
    entity.setAggregateType(DocumentUploadedEvent.AGGREGATE_TYPE);
    entity.setAggregateId(event.documentId());
    entity.setEventType(DocumentUploadedEvent.EVENT_TYPE);
    entity.setPayload(toJson(event));
    entity.setCreatedAt(Instant.now());
    entity.setPublishedAt(null);
    repository.save(entity);
  }

  @Override
  public List<OutboxMessage> findUnpublished(int batchSize) {
    return List.ofAll(repository.findUnpublished(batchSize)).map(PersistenceMapper::toDomain);
  }

  @Override
  public void markPublished(OutboxMessageId id) {
    repository
        .findById(id.value())
        .ifPresent(
            entity -> {
              entity.setPublishedAt(Instant.now());
              repository.save(entity);
            });
  }

  private String toJson(DocumentUploadedEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize outbox payload", e);
    }
  }
}
