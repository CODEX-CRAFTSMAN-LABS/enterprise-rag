package com.enterprise.rag.common.platform;

/** Platform guardrails — rate limiting and tenant header policy. */
public record PlatformProperties(
    boolean rateLimitEnabled,
    int requestsPerMinute,
    String tenantHeaderName,
    boolean tenantHeaderRequired) {

  public static PlatformProperties defaults() {
    return new PlatformProperties(true, 60, "X-Tenant-Id", true);
  }
}
