package com.enterprise.rag.query.domain;

/** Raw HTTP request before validation. */
public record AskRequest(String tenantId, String question, Integer topK) {}
