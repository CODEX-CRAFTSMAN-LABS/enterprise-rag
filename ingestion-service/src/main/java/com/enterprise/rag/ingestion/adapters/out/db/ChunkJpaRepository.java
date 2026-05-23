package com.enterprise.rag.ingestion.adapters.out.db;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChunkJpaRepository extends JpaRepository<ChunkEntity, UUID> {

  List<ChunkEntity> findByDocumentIdAndTenantIdOrderByChunkIndexAsc(
      UUID documentId, String tenantId);

  void deleteByDocumentId(UUID documentId);
}
