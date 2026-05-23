package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.ingestion.domain.DocumentUploadedEvent;
import com.enterprise.rag.ingestion.domain.OutboxMessage;
import com.enterprise.rag.ingestion.domain.OutboxMessageId;
import io.vavr.collection.List;

public interface OutboxRepositoryPort {

  void enqueue(DocumentUploadedEvent event);

  List<OutboxMessage> findUnpublished(int batchSize);

  void markPublished(OutboxMessageId id);
}
