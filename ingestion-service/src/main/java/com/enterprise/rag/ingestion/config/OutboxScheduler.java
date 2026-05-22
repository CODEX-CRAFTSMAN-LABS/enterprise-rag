package com.enterprise.rag.ingestion.config;

import com.enterprise.rag.ingestion.application.OutboxPollerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingestion.outbox.enabled", havingValue = "true")
class OutboxScheduler {

  private final OutboxPollerService outboxPollerService;
  private final IngestionProperties properties;

  @Scheduled(fixedDelayString = "${ingestion.outbox.poll-interval-ms:2000}")
  void pollOutbox() {
    outboxPollerService.pollAndPublish(properties.outbox().batchSize());
  }
}
