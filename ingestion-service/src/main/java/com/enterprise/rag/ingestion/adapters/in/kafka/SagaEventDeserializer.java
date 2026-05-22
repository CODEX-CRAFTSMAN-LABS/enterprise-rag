package com.enterprise.rag.ingestion.adapters.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaEventDeserializer {

  private final ObjectMapper objectMapper;

  public <T> T read(String payload, Class<T> type) {
    return Try.of(() -> objectMapper.readValue(unwrapJsonString(payload), type))
        .getOrElseThrow(ex -> new IllegalArgumentException("Failed to deserialize saga event", ex));
  }

  /** Handles legacy messages double-encoded by JsonSerializer on String payloads. */
  private String unwrapJsonString(String payload)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    String trimmed = payload.trim();
    if (trimmed.startsWith("\"")) {
      return objectMapper.readValue(trimmed, String.class);
    }
    return payload;
  }
}
