package com.enterprise.rag.query.domain;

import io.vavr.collection.List;

/** Pure RAG prompt construction — no IO, deterministic. */
public final class RagPromptBuilder {

  private RagPromptBuilder() {}

  public static String build(String question, List<RetrievedChunk> chunks) {
    StringBuilder context = new StringBuilder();
    chunks.forEach(
        chunk ->
            context
                .append("[doc=")
                .append(chunk.documentId())
                .append(" chunk=")
                .append(chunk.chunkIndex())
                .append(" score=")
                .append(String.format("%.4f", chunk.score()))
                .append("]\n")
                .append(chunk.content())
                .append("\n\n"));

    return """
      You are an enterprise document assistant. Answer ONLY using the context below.
      If the answer is not in the context, say you do not have enough information.

      Context:
      %s

      Question:
      %s

      Answer concisely and cite document ids when relevant.
      """
        .formatted(context.toString().trim(), question.trim());
  }
}
