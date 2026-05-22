package com.enterprise.rag.common.platform;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Dev/single-node token bucket — production uses Redis-backed limiter. */
public final class InMemoryTenantRateLimiter implements TenantRateLimiter {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final int requestsPerMinute;

  public InMemoryTenantRateLimiter(int requestsPerMinute) {
    this.requestsPerMinute = requestsPerMinute;
  }

  @Override
  public boolean tryConsume(String tenantId) {
    Bucket bucket =
        buckets.computeIfAbsent(
            tenantId,
            id ->
                Bucket.builder()
                    .addLimit(
                        Bandwidth.builder()
                            .capacity(requestsPerMinute)
                            .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                            .build())
                    .build());
    return bucket.tryConsume(1);
  }
}
