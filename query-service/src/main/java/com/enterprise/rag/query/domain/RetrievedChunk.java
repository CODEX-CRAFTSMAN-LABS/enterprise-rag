package com.enterprise.rag.query.domain;

/** A chunk returned from vector similarity search. */
public record RetrievedChunk(String documentId, int chunkIndex, String content, double score) {}
