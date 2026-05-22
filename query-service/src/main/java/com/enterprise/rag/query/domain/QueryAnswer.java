package com.enterprise.rag.query.domain;

import io.vavr.collection.List;

public record QueryAnswer(String answer, List<Citation> citations) {

  public record Citation(String documentId, int chunkIndex, double score) {

    public static Citation from(RetrievedChunk chunk) {
      return new Citation(chunk.documentId(), chunk.chunkIndex(), chunk.score());
    }
  }
}
