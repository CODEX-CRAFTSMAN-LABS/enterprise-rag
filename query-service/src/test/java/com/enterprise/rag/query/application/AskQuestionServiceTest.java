package com.enterprise.rag.query.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.common.error.ValidationError;
import com.enterprise.rag.query.config.QueryMetrics;
import com.enterprise.rag.query.config.QueryProperties;
import com.enterprise.rag.query.domain.AskRequest;
import com.enterprise.rag.query.domain.RetrievedChunk;
import com.enterprise.rag.query.ports.out.EmbeddingCachePort;
import com.enterprise.rag.query.ports.out.EmbeddingPort;
import com.enterprise.rag.query.ports.out.LlmPort;
import com.enterprise.rag.query.ports.out.RetrievalPort;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AskQuestionServiceTest {

  @Mock private EmbeddingCachePort embeddingCachePort;
  @Mock private EmbeddingPort embeddingPort;
  @Mock private RetrievalPort retrievalPort;
  @Mock private LlmPort llmPort;
  @Mock private QueryMetrics queryMetrics;

  private AskQuestionService service;

  @BeforeEach
  void setUp() {
    service =
        new AskQuestionService(
            embeddingCachePort,
            embeddingPort,
            retrievalPort,
            llmPort,
            new QueryProperties(new QueryProperties.Cache(Duration.ofHours(1))),
            queryMetrics);
  }

  @Test
  void should_useCache_when_embeddingCached() {
    float[] cached = new float[] {0.1f, 0.2f};
    when(embeddingCachePort.get(any())).thenReturn(Option.of(cached));
    when(retrievalPort.retrieve(any(), eq(cached), anyInt()))
        .thenReturn(Either.right(List.of(new RetrievedChunk("d1", 0, "context", 0.9))));
    when(llmPort.complete(any())).thenReturn(Either.right("Answer from LLM"));

    var result = service.ask(new AskRequest("acme-corp", "What is the refund policy?", 3));

    assertThat(result.isRight()).isTrue();
    verify(embeddingPort, never()).embed(any());
  }

  @Test
  void should_callEmbedder_when_cacheMiss() {
    float[] embedding = new float[] {0.5f, 0.6f};
    when(embeddingCachePort.get(any())).thenReturn(Option.none());
    when(embeddingPort.embed(any())).thenReturn(Either.right(embedding));
    when(retrievalPort.retrieve(any(), eq(embedding), anyInt()))
        .thenReturn(Either.right(List.of(new RetrievedChunk("d1", 0, "context", 0.8))));
    when(llmPort.complete(any())).thenReturn(Either.right("Generated answer"));

    var result = service.ask(new AskRequest("acme-corp", "Summarize policy", 5));

    assertThat(result.isRight()).isTrue();
    assertThat(result.get().answer()).isEqualTo("Generated answer");
    verify(embeddingCachePort).put(any(), eq(embedding), eq(Duration.ofHours(1)));
  }

  @Test
  void should_returnValidationError_when_questionBlank() {
    var result = service.ask(new AskRequest("acme-corp", "  ", 5));
    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isInstanceOf(ValidationError.class);
  }
}
