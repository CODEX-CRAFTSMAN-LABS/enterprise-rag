package com.enterprise.rag.common.error;

import io.vavr.collection.Seq;

public record ValidationError(Seq<String> violations) implements AppError {

  @Override
  public String message() {
    return "Validation failed: " + String.join("; ", violations);
  }
}
