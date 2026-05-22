package com.enterprise.rag.ingestion.domain;

import io.vavr.control.Option;
import java.time.Instant;

/** Immutable document aggregate (write model). */
public record Document(
    DocumentId id,
    TenantId tenantId,
    String filename,
    String contentType,
    long sizeBytes,
    DocumentStatus status,
    byte[] content,
    Option<String> extractedText,
    Instant createdAt,
    Instant updatedAt) {}
