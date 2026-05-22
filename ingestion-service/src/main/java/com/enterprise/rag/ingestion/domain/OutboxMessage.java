package com.enterprise.rag.ingestion.domain;

import io.vavr.control.Option;
import java.time.Instant;

public record OutboxMessage(
    OutboxMessageId id,
    String aggregateType,
    String aggregateId,
    String eventType,
    String payloadJson,
    Instant createdAt,
    Option<Instant> publishedAt) {

  public boolean isPublished() {
    return publishedAt.isDefined();
  }
}
