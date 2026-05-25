package com.enterprise.rag.query.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(QueryProperties.class)
public class QueryConfig {

  @Bean
  RestClient ollamaRestClient(
      @Value("${spring.ai.ollama.base-url}") String baseUrl,
      @Value("${query.ollama.request-timeout:60s}") Duration requestTimeout,
      ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(requestTimeout);

    return restClientBuilderProvider
        .getIfAvailable(RestClient::builder)
        .clone()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .build();
  }

  @Bean
  OllamaApi ollamaApi(
      @Value("${spring.ai.ollama.base-url}") String baseUrl,
      RestClient ollamaRestClient,
      ObjectProvider<WebClient.Builder> webClientBuilderProvider) {
    return OllamaApi.builder()
        .baseUrl(baseUrl)
        .restClientBuilder(ollamaRestClient.mutate())
        .webClientBuilder(webClientBuilderProvider.getIfAvailable(WebClient::builder))
        .build();
  }

  @Bean
  CircuitBreaker embeddingCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("embedding");
  }

  @Bean
  CircuitBreaker llmCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("llm");
  }
}
