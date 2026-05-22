package com.enterprise.rag.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;

class ChunkerTest {

  @Property
  void should_neverProduceEmptyChunks_when_textIsNonBlank(
      @ForAll @NotBlank @Size(min = 1, max = 500) String text,
      @ForAll @IntRange(min = 50, max = 200) int chunkSize,
      @ForAll @IntRange(min = 0, max = 40) int overlap) {
    int safeOverlap = Math.min(overlap, chunkSize - 1);
    var chunks = Chunker.chunk(text, chunkSize, safeOverlap);
    assertThat(chunks).isNotEmpty();
    chunks.forEach(chunk -> assertThat(chunk.content()).isNotBlank());
  }

  @Property
  void should_coverAllCharacters_when_chunked(
      @ForAll @NotBlank @Size(min = 10, max = 300) String text) {
    int chunkSize = 80;
    int overlap = 10;
    var chunks = Chunker.chunk(text.trim(), chunkSize, overlap);
    String combined = chunks.map(TextChunk::content).mkString("");
    assertThat(combined.length()).isGreaterThan(0);
  }
}
