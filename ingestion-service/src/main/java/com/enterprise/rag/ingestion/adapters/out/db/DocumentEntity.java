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
@Table(name = "documents")
@Getter
@Setter
class DocumentEntity {

  @Id private UUID id;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(nullable = false, length = 512)
  private String filename;

  @Column(name = "content_type", nullable = false, length = 128)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "content", nullable = false, columnDefinition = "BYTEA")
  private byte[] content;

  @Column(name = "extracted_text")
  private String extractedText;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
