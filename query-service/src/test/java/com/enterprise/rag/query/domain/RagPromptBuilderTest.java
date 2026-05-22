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
  }
}
