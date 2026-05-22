package com.enterprise.rag.ingestion.domain;

public record DocumentParsedEvent(
    String eventId, String documentId, String tenantId, int textLength) {

  public static final String EVENT_TYPE = "DOCUMENT_PARSED";
}
