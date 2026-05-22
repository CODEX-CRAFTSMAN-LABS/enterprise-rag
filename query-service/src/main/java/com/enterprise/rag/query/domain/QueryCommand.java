package com.enterprise.rag.query.domain;

/** Validated ask request — immutable input to the RAG pipeline. */
public record QueryCommand(TenantId tenantId, String question, int topK) {}
