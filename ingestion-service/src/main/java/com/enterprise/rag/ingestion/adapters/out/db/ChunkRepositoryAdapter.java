package com.enterprise.rag.ingestion.adapters.out.db;

import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.TextChunk;
import com.enterprise.rag.ingestion.ports.out.ChunkRepositoryPort;
import io.vavr.collection.List;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ChunkRepositoryAdapter implements ChunkRepositoryPort {

  private final ChunkJpaRepository repository;

  @Override
  public void saveAll(DocumentId documentId, TenantId tenantId, List<TextChunk> chunks) {
    repository.deleteByDocumentId(documentId.value());
    chunks.forEach(
        chunk -> {
          ChunkEntity entity = new ChunkEntity();
          entity.setId(UUID.randomUUID());
          entity.setDocumentId(documentId.value());
          entity.setTenantId(tenantId.value());
          entity.setChunkIndex(chunk.index());
          entity.setContent(chunk.content());
          entity.setCreatedAt(Instant.now());
          repository.save(entity);
        });
  }

  @Override
  public List<TextChunk> findByDocument(DocumentId documentId, TenantId tenantId) {
    return List.ofAll(
            repository.findByDocumentIdAndTenantIdOrderByChunkIndexAsc(
                documentId.value(), tenantId.value()))
        .map(entity -> new TextChunk(entity.getChunkIndex(), entity.getContent()));
  }
}
