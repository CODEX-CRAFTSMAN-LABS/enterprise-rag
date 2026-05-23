package com.enterprise.rag.ingestion.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.ingestion.domain.OutboxMessage;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.ports.out.EventPublisherPort;
import com.enterprise.rag.ingestion.ports.out.OutboxRepositoryPort;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPollerServiceTest {

  @Mock private OutboxRepositoryPort outboxRepository;
  @Mock private EventPublisherPort eventPublisher;

  @InjectMocks private OutboxPollerService service;

  @Test
  void should_markPublished_when_publishSucceeds() {
    OutboxMessage message = sampleMessage();
    when(outboxRepository.findUnpublished(5)).thenReturn(List.of(message));
    when(eventPublisher.publish(message)).thenReturn(Either.right(null));

    service.pollAndPublish(5);

    verify(outboxRepository).markPublished(message.id());
  }

  @Test
  void should_notMarkPublished_when_publishFails() {
    OutboxMessage message = sampleMessage();
    when(outboxRepository.findUnpublished(5)).thenReturn(List.of(message));
    when(eventPublisher.publish(message))
        .thenReturn(Either.left(new IngestionError("outbox", "broker down", null)));

    service.pollAndPublish(5);

    verify(outboxRepository, never()).markPublished(any());
  }

  private static OutboxMessage sampleMessage() {
    return new OutboxMessage(
        OutboxMessageId.generate(),
        "Document",
        "doc-1",
        "DOCUMENT_UPLOADED",
        "{}",
        Instant.parse("2025-01-01T00:00:00Z"),
        Option.none());
  }
}
