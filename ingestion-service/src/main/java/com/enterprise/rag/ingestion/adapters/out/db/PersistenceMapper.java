package com.enterprise.rag.ingestion.adapters.out.db;

import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.OutboxMessage;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import com.enterprise.rag.ingestion.domain.TenantId;
import io.vavr.control.Option;

final class PersistenceMapper {

  private PersistenceMapper() {}

  static DocumentEntity toEntity(Document document) {
    DocumentEntity entity = new DocumentEntity();
    entity.setId(document.id().value());
    entity.setTenantId(document.tenantId().value());
    entity.setFilename(document.filename());
    entity.setContentType(document.contentType());
    entity.setSizeBytes(document.sizeBytes());
    entity.setStatus(document.status().name());
    entity.setContent(document.content());
    entity.setExtractedText(document.extractedText().getOrNull());
    entity.setCreatedAt(document.createdAt());
    entity.setUpdatedAt(document.updatedAt());
    return entity;
  }

  static Document toDomain(DocumentEntity entity) {
    return new Document(
        new DocumentId(entity.getId()),
        new TenantId(entity.getTenantId()),
        entity.getFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        DocumentStatus.valueOf(entity.getStatus()),
        entity.getContent(),
        Option.of(entity.getExtractedText()),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  static OutboxMessage toDomain(OutboxEventEntity entity) {
    return new OutboxMessage(
        new OutboxMessageId(entity.getId()),
        entity.getAggregateType(),
        entity.getAggregateId(),
        entity.getEventType(),
        entity.getPayload(),
        entity.getCreatedAt(),
        Option.of(entity.getPublishedAt()));
  }
}
