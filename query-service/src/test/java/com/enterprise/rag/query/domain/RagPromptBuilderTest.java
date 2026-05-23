package com.enterprise.rag.query.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.vavr.collection.List;
import org.junit.jupiter.api.Test;

class RagPromptBuilderTest {

  @Test
  void should_includeQuestionAndContext_when_chunksPresent() {
    var chunks = List.of(new RetrievedChunk("doc-1", 0, "Refund within 30 days.", 0.92));
    String prompt = RagPromptBuilder.build("What is the refund policy?", chunks);
    assertThat(prompt).contains("Refund within 30 days");
    assertThat(prompt).contains("What is the refund policy?");
    assertThat(prompt).contains("[doc=doc-1 chunk=0 score=0.9200]");
  }

  @Test
  void should_trimQuestionAndJoinMultipleChunks() {
    var chunks =
        List.of(
            new RetrievedChunk("doc-a", 0, "Section A", 0.5),
            new RetrievedChunk("doc-b", 1, "Section B", 0.7));
    String prompt = RagPromptBuilder.build("  summarize  ", chunks);
    assertThat(prompt).contains("Section A");
    assertThat(prompt).contains("Section B");
    assertThat(prompt).contains("summarize");
  }
}
