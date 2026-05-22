package com.enterprise.rag.ingestion.domain;

/** Kafka saga entry event — published via transactional outbox. */
public record DocumentUploadedEvent(
    String eventId,
    String documentId,
    String tenantId,
    String filename,
    String contentType,
    long sizeBytes) {

  public static final String EVENT_TYPE = "DOCUMENT_UPLOADED";
  public static final String AGGREGATE_TYPE = "Document";

  public static DocumentUploadedEvent from(Document document, String eventId) {
    return new DocumentUploadedEvent(
        eventId,
        document.id().asString(),
        document.tenantId().value(),
        document.filename(),
        document.contentType(),
        document.sizeBytes());
  }
}
