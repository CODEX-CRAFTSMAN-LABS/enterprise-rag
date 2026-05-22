package com.enterprise.rag.query.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QueryProperties.class)
public class QueryConfig {

  @Bean
  CircuitBreaker embeddingCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("embedding");
  }

  @Bean
  CircuitBreaker llmCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("llm");
  }
}
