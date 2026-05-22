package com.enterprise.rag.common.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryTenantRateLimiterTest {

  @Test
  void should_blockAfterLimitExceeded() {
    var limiter = new InMemoryTenantRateLimiter(3);
    assertThat(limiter.tryConsume("tenant-a")).isTrue();
    assertThat(limiter.tryConsume("tenant-a")).isTrue();
    assertThat(limiter.tryConsume("tenant-a")).isTrue();
    assertThat(limiter.tryConsume("tenant-a")).isFalse();
  }

  @Test
  void should_isolateTenants() {
    var limiter = new InMemoryTenantRateLimiter(1);
    assertThat(limiter.tryConsume("tenant-a")).isTrue();
    assertThat(limiter.tryConsume("tenant-b")).isTrue();
    assertThat(limiter.tryConsume("tenant-a")).isFalse();
  }
}
