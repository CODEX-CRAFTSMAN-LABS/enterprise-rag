package com.enterprise.rag.ingestion.domain;

import io.vavr.collection.List;

/** Pure text chunking — deterministic, no IO. */
public final class Chunker {

  private Chunker() {}

  public static List<TextChunk> chunk(String text, int chunkSize, int overlap) {
    if (text == null || text.isBlank()) {
      return List.empty();
    }
    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize must be positive");
    }
    if (overlap < 0 || overlap >= chunkSize) {
      throw new IllegalArgumentException("overlap must be >= 0 and < chunkSize");
    }

    String normalized = text.trim();
    List<TextChunk> chunks = List.empty();
    int index = 0;
    int start = 0;

    while (start < normalized.length()) {
      int end = Math.min(start + chunkSize, normalized.length());
      String slice = normalized.substring(start, end).trim();
      if (!slice.isEmpty()) {
        chunks = chunks.append(new TextChunk(index++, slice));
      }
      if (end >= normalized.length()) {
        break;
      }
      start = end - overlap;
    }
    return chunks;
  }
}
