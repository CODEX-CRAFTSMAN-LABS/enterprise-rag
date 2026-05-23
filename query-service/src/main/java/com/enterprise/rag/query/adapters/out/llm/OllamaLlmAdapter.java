package com.enterprise.rag.query.adapters.out.llm;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.LlmError;
import com.enterprise.rag.query.ports.out.LlmPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

/** Registered via {@link com.enterprise.rag.query.config.QueryAdapterConfig}. */
@RequiredArgsConstructor
public class OllamaLlmAdapter implements LlmPort {

  private final ChatModel chatModel;
  private final CircuitBreaker llmCircuitBreaker;

  @Override
  public Either<AppError, String> complete(String prompt) {
    return Try.of(
            () ->
                CircuitBreaker.decorateSupplier(
                        llmCircuitBreaker,
                        () -> chatModel.call(new Prompt(prompt)).getResult().getOutput().getText())
                    .get())
        .toEither()
        .mapLeft(t -> (AppError) LlmError.fromThrowable(t));
  }
}
