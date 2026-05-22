package com.enterprise.rag.common.error;

/** Expected on cache-aside miss; often handled internally rather than returned to clients. */
public record CacheMiss(String key) implements AppError {

  @Override
  public String message() {
    return "Cache miss for key: " + key;
  }
}
