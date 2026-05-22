package com.enterprise.rag.common.error;

public record NotFoundError(String resource, String id) implements AppError {

  @Override
  public String message() {
    return "%s not found: %s".formatted(resource, id);
  }
}
