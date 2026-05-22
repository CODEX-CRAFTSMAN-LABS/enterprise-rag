package com.enterprise.rag.common.error;

public record RetrievalError(String detail, Throwable cause) implements AppError {

  @Override
  public String message() {
    return "Retrieval failed: " + detail;
  }
}
