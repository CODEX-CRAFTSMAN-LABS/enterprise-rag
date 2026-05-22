package com.enterprise.rag.query.config;

import com.enterprise.rag.common.platform.PlatformProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public record PlatformSpringProperties(RateLimit rateLimit, Tenant tenant) {

  public record RateLimit(boolean enabled, int requestsPerMinute, boolean redisBacked) {}

  public record Tenant(String headerName, boolean required) {}

  public PlatformProperties toPlatformProperties() {
    return new PlatformProperties(
        rateLimit.enabled(), rateLimit.requestsPerMinute(), tenant.headerName(), tenant.required());
  }
}
