package com.enterprise.rag.ingestion.config;

import com.enterprise.rag.common.platform.InMemoryTenantRateLimiter;
import com.enterprise.rag.common.platform.TenantRateLimitFilter;
import com.enterprise.rag.common.platform.TenantRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(PlatformSpringProperties.class)
public class PlatformConfig {

  @Bean
  @ConditionalOnProperty(
      prefix = "platform.rate-limit",
      name = "redis-backed",
      havingValue = "false")
  TenantRateLimiter inMemoryTenantRateLimiter(PlatformSpringProperties properties) {
    return new InMemoryTenantRateLimiter(properties.rateLimit().requestsPerMinute());
  }

  @Bean
  @Primary
  @ConditionalOnProperty(
      prefix = "platform.rate-limit",
      name = "redis-backed",
      havingValue = "true")
  @ConditionalOnBean(StringRedisTemplate.class)
  TenantRateLimiter redisTenantRateLimiter(
      StringRedisTemplate redisTemplate, PlatformSpringProperties properties) {
    return new RedisTenantRateLimiter(redisTemplate, properties);
  }

  /** Fallback when redis-backed=true but Redis auto-config is not ready. */
  @Bean
  @ConditionalOnMissingBean(TenantRateLimiter.class)
  TenantRateLimiter fallbackTenantRateLimiter(PlatformSpringProperties properties) {
    return new InMemoryTenantRateLimiter(properties.rateLimit().requestsPerMinute());
  }

  @Bean
  FilterRegistrationBean<TenantRateLimitFilter> tenantRateLimitFilter(
      TenantRateLimiter rateLimiter,
      PlatformSpringProperties properties,
      ObjectMapper objectMapper) {
    FilterRegistrationBean<TenantRateLimitFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(
        new TenantRateLimitFilter(rateLimiter, properties.toPlatformProperties(), objectMapper));
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }
}
