package com.enterprise.rag.ingestion.adapters.out.db;

import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import io.vavr.control.Option;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DocumentRepositoryAdapter implements DocumentRepositoryPort {

  private final DocumentJpaRepository repository;

  @Override
  public Document save(Document document) {
    DocumentEntity saved = repository.save(PersistenceMapper.toEntity(document));
    return PersistenceMapper.toDomain(saved);
  }

  @Override
  public Option<Document> findById(DocumentId id, TenantId tenantId) {
    return Option.ofOptional(repository.findByIdAndTenantId(id.value(), tenantId.value()))
        .map(PersistenceMapper::toDomain);
  }

  @Override
  public Document updateExtractedText(DocumentId id, TenantId tenantId, String extractedText) {
    DocumentEntity entity = loadEntity(id, tenantId);
    entity.setExtractedText(extractedText);
    entity.setStatus(DocumentStatus.PROCESSING.name());
    entity.setUpdatedAt(Instant.now());
    return PersistenceMapper.toDomain(repository.save(entity));
  }

  @Override
  public Document updateStatus(DocumentId id, TenantId tenantId, DocumentStatus status) {
    DocumentEntity entity = loadEntity(id, tenantId);
    entity.setStatus(status.name());
    entity.setUpdatedAt(Instant.now());
    return PersistenceMapper.toDomain(repository.save(entity));
  }

  private DocumentEntity loadEntity(DocumentId id, TenantId tenantId) {
    return repository
        .findByIdAndTenantId(id.value(), tenantId.value())
        .orElseThrow(() -> new IllegalStateException("Document not found: " + id.asString()));
  }
}
