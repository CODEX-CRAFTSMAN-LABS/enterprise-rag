package com.enterprise.rag.common.platform;

/** Token-bucket rate limiting per tenant — System design pattern: Rate limiting */
public interface TenantRateLimiter {

  /**
   * @return true if request is allowed
   */
  boolean tryConsume(String tenantId);
}
