package com.enterprise.rag.ingestion.adapters.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SagaEventDeserializerTest {

  private SagaEventDeserializer deserializer;

  @BeforeEach
  void setUp() {
    deserializer = new SagaEventDeserializer(new ObjectMapper());
  }

  @Test
  void should_readPlainJson() {
    String json =
        """
        {"eventId":"e1","documentId":"d1","tenantId":"t1","filename":"f.txt","contentType":"text/plain","sizeBytes":5}
        """;

    DocumentUploadedEvent event = deserializer.read(json, DocumentUploadedEvent.class);

    assertThat(event.documentId()).isEqualTo("d1");
    assertThat(event.tenantId()).isEqualTo("t1");
  }

  @Test
  void should_unwrapDoubleEncodedJson() throws Exception {
    String inner =
        """
        {"eventId":"e2","documentId":"d2","tenantId":"t2","filename":"f.txt","contentType":"text/plain","sizeBytes":3}
        """;
    String wrapped = new ObjectMapper().writeValueAsString(inner.trim());

    DocumentUploadedEvent event = deserializer.read(wrapped, DocumentUploadedEvent.class);

    assertThat(event.documentId()).isEqualTo("d2");
  }

  @Test
  void should_fail_when_payloadInvalid() {
    assertThatThrownBy(() -> deserializer.read("not-json", DocumentUploadedEvent.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Failed to deserialize");
  }
}
