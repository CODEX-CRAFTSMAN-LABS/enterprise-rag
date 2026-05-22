package com.enterprise.rag.ingestion.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(Outbox outbox, Saga saga, Chunking chunking, Kafka kafka) {

  public record Outbox(boolean enabled, int batchSize, Duration pollInterval) {}

  public record Saga(boolean enabled) {}

  public record Chunking(int size, int overlap) {}

  public record Kafka(
      String documentUploadedTopic,
      String documentParsedTopic,
      String documentChunkedTopic,
      String documentIngestionFailedTopic) {}
}
