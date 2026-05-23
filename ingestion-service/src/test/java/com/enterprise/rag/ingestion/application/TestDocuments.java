package com.enterprise.rag.ingestion.application;

import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import io.vavr.control.Option;
import java.time.Instant;

final class TestDocuments {

  private TestDocuments() {}

  static Document document(
      DocumentId id, TenantId tenantId, DocumentStatus status, Option<String> extractedText) {
    return new Document(
        id,
        tenantId,
        "notes.txt",
        "text/plain",
        5,
        status,
        "hello".getBytes(),
        extractedText,
        Instant.parse("2025-01-01T00:00:00Z"),
        Instant.parse("2025-01-01T00:00:00Z"));
  }
}
