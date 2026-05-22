package com.enterprise.rag.ingestion.domain;

import java.util.UUID;

public record OutboxMessageId(UUID value) {

  public static OutboxMessageId generate() {
    return new OutboxMessageId(UUID.randomUUID());
  }

  public String asString() {
    return value.toString();
  }
}
