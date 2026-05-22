package com.enterprise.rag.common.error;

public record RateLimitError(String tenantId, long retryAfterSeconds) implements AppError {

  @Override
  public String message() {
    return "Rate limit exceeded for tenant: " + tenantId;
  }
}
