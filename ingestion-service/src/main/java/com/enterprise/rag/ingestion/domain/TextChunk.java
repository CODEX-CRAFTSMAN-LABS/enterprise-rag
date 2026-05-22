package com.enterprise.rag.ingestion.domain;

/** Immutable chunk of extracted document text. */
public record TextChunk(int index, String content) {}
