package com.enterprise.rag.query.config;

import com.enterprise.rag.query.adapters.out.llm.OllamaEmbeddingAdapter;
import com.enterprise.rag.query.adapters.out.llm.OllamaLlmAdapter;
import com.enterprise.rag.query.adapters.out.redis.InMemoryEmbeddingCacheAdapter;
import com.enterprise.rag.query.adapters.out.redis.RedisEmbeddingCacheAdapter;
import com.enterprise.rag.query.adapters.out.vector.PgvectorRetrievalAdapter;
import com.enterprise.rag.query.ports.out.EmbeddingCachePort;
import com.enterprise.rag.query.ports.out.EmbeddingPort;
import com.enterprise.rag.query.ports.out.LlmPort;
import com.enterprise.rag.query.ports.out.RetrievalPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class QueryAdapterConfig {

  @Bean
  @Primary
  @ConditionalOnBean(StringRedisTemplate.class)
  EmbeddingCachePort redisEmbeddingCachePort(
      StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    return new RedisEmbeddingCacheAdapter(redisTemplate, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean(EmbeddingCachePort.class)
  EmbeddingCachePort inMemoryEmbeddingCachePort() {
    return new InMemoryEmbeddingCacheAdapter();
  }

  @Bean
  @ConditionalOnMissingBean(EmbeddingPort.class)
  EmbeddingPort embeddingPort(
      OllamaEmbeddingModel embeddingModel, CircuitBreaker embeddingCircuitBreaker) {
    return new OllamaEmbeddingAdapter(embeddingModel, embeddingCircuitBreaker);
  }

  @Bean
  @ConditionalOnMissingBean(LlmPort.class)
  LlmPort llmPort(
      @Value("${spring.ai.ollama.base-url}") String baseUrl,
      @Value("${query.ollama.request-timeout:60s}") java.time.Duration requestTimeout,
      ObjectMapper objectMapper,
      @Value("${spring.ai.ollama.chat.options.model}") String model,
      CircuitBreaker llmCircuitBreaker) {
    return new OllamaLlmAdapter(baseUrl, requestTimeout, objectMapper, model, llmCircuitBreaker);
  }

  @Bean
  @ConditionalOnMissingBean(RetrievalPort.class)
  RetrievalPort retrievalPort(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    return new PgvectorRetrievalAdapter(jdbcTemplate, objectMapper);
  }
}
