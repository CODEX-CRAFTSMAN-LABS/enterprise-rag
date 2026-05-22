package com.enterprise.rag.ingestion.domain;

import java.util.UUID;

public record DocumentId(UUID value) {

  public static DocumentId generate() {
    return new DocumentId(UUID.randomUUID());
  }

  public static DocumentId of(String raw) {
    return new DocumentId(UUID.fromString(raw));
  }

  public String asString() {
    return value.toString();
  }
}
