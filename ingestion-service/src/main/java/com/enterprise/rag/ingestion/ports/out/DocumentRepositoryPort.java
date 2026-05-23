package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import io.vavr.control.Option;

public interface DocumentRepositoryPort {

  Document save(Document document);

  Option<Document> findById(DocumentId id, TenantId tenantId);

  Document updateExtractedText(DocumentId id, TenantId tenantId, String extractedText);

  Document updateStatus(DocumentId id, TenantId tenantId, DocumentStatus status);
}
