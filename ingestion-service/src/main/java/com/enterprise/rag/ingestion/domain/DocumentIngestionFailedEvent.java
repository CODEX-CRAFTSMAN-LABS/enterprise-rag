package com.enterprise.rag.ingestion.domain;

public record DocumentIngestionFailedEvent(
    String eventId, String documentId, String tenantId, String step, String reason) {

  public static final String EVENT_TYPE = "DOCUMENT_INGESTION_FAILED";
}
