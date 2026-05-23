package com.enterprise.rag.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmbeddingCacheKeysTest {

  @Test
  void should_normalizeQuestionCaseAndWhitespace() {
    String key1 = EmbeddingCacheKeys.forQuestion("acme", "  What is Policy? ");
    String key2 = EmbeddingCacheKeys.forQuestion("acme", "what is policy?");
    assertThat(key1).isEqualTo(key2);
  }

  @Test
  void should_includeTenantInKey() {
    String key = EmbeddingCacheKeys.forQuestion("tenant-a", "hello");
    assertThat(key).startsWith("tenant:tenant-a:embed:");
    assertThat(key).hasSizeGreaterThan("tenant:tenant-a:embed:".length());
  }
}
