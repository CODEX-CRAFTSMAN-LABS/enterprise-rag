package com.enterprise.rag.query.adapters.out.llm;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.EmbeddingError;
import com.enterprise.rag.query.ports.out.EmbeddingPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;

/** Registered via {@link com.enterprise.rag.query.config.QueryAdapterConfig}. */
@RequiredArgsConstructor
public class OllamaEmbeddingAdapter implements EmbeddingPort {

  private final EmbeddingModel embeddingModel;
  private final CircuitBreaker embeddingCircuitBreaker;

  @Override
  public Either<AppError, float[]> embed(String text) {
    return Try.of(
            () ->
                CircuitBreaker.decorateSupplier(
                        embeddingCircuitBreaker, () -> embeddingModel.embed(text))
                    .get())
        .toEither()
        .mapLeft(t -> (AppError) EmbeddingError.fromThrowable(t));
  }
}
