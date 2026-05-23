package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.TextChunk;
import io.vavr.collection.List;

public interface ChunkRepositoryPort {

  void saveAll(DocumentId documentId, TenantId tenantId, List<TextChunk> chunks);

  List<TextChunk> findByDocument(DocumentId documentId, TenantId tenantId);
}
