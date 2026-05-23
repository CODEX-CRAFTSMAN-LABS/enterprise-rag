package com.enterprise.rag.ingestion.adapters.out.db;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

  @Query(
      value =
          "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at ASC LIMIT :limit",
      nativeQuery = true)
  List<OutboxEventEntity> findUnpublished(@Param("limit") int limit);
}
