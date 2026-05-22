package com.enterprise.rag.ingestion.domain;

public record DocumentChunkedEvent(
    String eventId, String documentId, String tenantId, int chunkCount) {

  public static final String EVENT_TYPE = "DOCUMENT_CHUNKED";
}
