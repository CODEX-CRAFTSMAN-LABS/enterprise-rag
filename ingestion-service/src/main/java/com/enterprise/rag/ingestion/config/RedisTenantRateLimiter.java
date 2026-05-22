package com.enterprise.rag.ingestion.config;

import com.enterprise.rag.common.platform.TenantRateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Distributed rate limit via Redis INCR (registered as @Bean in PlatformConfig). */
@RequiredArgsConstructor
class RedisTenantRateLimiter implements TenantRateLimiter {

  private final StringRedisTemplate redisTemplate;
  private final PlatformSpringProperties properties;

  @Override
  public boolean tryConsume(String tenantId) {
    int limit = properties.rateLimit().requestsPerMinute();
    String window = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
    String key = "ratelimit:" + tenantId + ":" + window;
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redisTemplate.expire(key, Duration.ofMinutes(2));
    }
    return count != null && count <= limit;
  }
}
