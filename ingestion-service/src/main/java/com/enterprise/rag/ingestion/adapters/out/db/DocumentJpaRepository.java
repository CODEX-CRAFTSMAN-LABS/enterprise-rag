package com.enterprise.rag.ingestion.adapters.out.db;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {

  Optional<DocumentEntity> findByIdAndTenantId(UUID id, String tenantId);
}
