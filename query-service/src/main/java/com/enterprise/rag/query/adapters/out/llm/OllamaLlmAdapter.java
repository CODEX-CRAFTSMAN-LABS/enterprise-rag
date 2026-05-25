package com.enterprise.rag.query.adapters.out.llm;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.LlmError;
import com.enterprise.rag.query.ports.out.LlmPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Registered via {@link com.enterprise.rag.query.config.QueryAdapterConfig}. */
@RequiredArgsConstructor
public class OllamaLlmAdapter implements LlmPort {

  private final String baseUrl;
  private final Duration requestTimeout;
  private final ObjectMapper objectMapper;
  private final String model;
  private final CircuitBreaker llmCircuitBreaker;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Override
  public Either<AppError, String> complete(String prompt) {
    return Try.of(
            () ->
                CircuitBreaker.decorateSupplier(llmCircuitBreaker, () -> extractContent(prompt))
                    .get())
        .toEither()
        .mapLeft(t -> (AppError) LlmError.fromThrowable(t));
  }

  private String extractContent(String prompt) {
    try {
      String requestBody =
          objectMapper.writeValueAsString(
              Map.of(
                  "model",
                  model,
                  "stream",
                  false,
                  "messages",
                  java.util.List.of(Map.of("role", "user", "content", prompt))));
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
              .timeout(requestTimeout)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new IllegalStateException(
            "Ollama chat failed with status " + response.statusCode() + ": " + response.body());
      }
      return objectMapper.readTree(response.body()).path("message").path("content").asText();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Ollama chat response", e);
    }
  }
}
