package com.enterprise.rag.common.error;

import java.util.Optional;

public record EmbeddingError(String detail, Throwable cause) implements AppError {

  public static EmbeddingError fromThrowable(Throwable t) {
    return new EmbeddingError(
        Optional.ofNullable(t.getMessage()).orElse(t.getClass().getSimpleName()), t);
  }

  @Override
  public String message() {
    return "Embedding failed: " + detail;
  }
}
