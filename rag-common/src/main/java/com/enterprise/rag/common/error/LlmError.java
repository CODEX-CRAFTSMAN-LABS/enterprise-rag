package com.enterprise.rag.common.error;

import java.util.Optional;

public record LlmError(String detail, Throwable cause) implements AppError {

  public static LlmError fromThrowable(Throwable t) {
    return new LlmError(
        Optional.ofNullable(t.getMessage()).orElse(t.getClass().getSimpleName()), t);
  }

  @Override
  public String message() {
    return "LLM call failed: " + detail;
  }
}
