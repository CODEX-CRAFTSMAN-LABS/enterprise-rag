package com.enterprise.rag.ingestion.adapters.out.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
class ChunkEntity {

  @Id private UUID id;

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
