package com.enterprise.rag.ingestion.domain;

/** Command to register a new document for async ingestion. */
public record UploadCommand(
    TenantId tenantId, String filename, String contentType, byte[] content) {}
