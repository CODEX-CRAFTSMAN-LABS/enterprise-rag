package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.ingestion.domain.OutboxMessage;
import com.enterprise.rag.ingestion.ports.out.EventPublisherPort;
import com.enterprise.rag.ingestion.ports.out.OutboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// System design pattern: Outbox — reliable async publish
// Reads unpublished rows and dispatches to Kafka; marks published after broker ack.

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPollerService {

  private final OutboxRepositoryPort outboxRepository;
  private final EventPublisherPort eventPublisher;

  public void pollAndPublish(int batchSize) {
    io.vavr.collection.List<OutboxMessage> pending = outboxRepository.findUnpublished(batchSize);
    pending.forEach(this::publishSingle);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void publishSingle(OutboxMessage message) {
    eventPublisher
        .publish(message)
        .peek(
            ignored -> {
              outboxRepository.markPublished(message.id());
              log.debug("Published outbox event id={}", message.id().asString());
            })
        .peekLeft(
            error ->
                log.warn(
                    "Failed to publish outbox id={}: {}",
                    message.id().asString(),
                    error.message()));
  }
}
