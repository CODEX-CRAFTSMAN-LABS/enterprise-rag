package com.enterprise.rag.ingestion.adapters.in.http.dto;

import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Document accepted for async ingestion")
public record DocumentUploadResponse(
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String documentId,
    @Schema(example = "PENDING") String status) {

  public static DocumentUploadResponse from(DocumentId id) {
    return new DocumentUploadResponse(id.asString(), DocumentStatus.PENDING.name());
  }
}
