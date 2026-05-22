package com.enterprise.rag.common.error;

public record IngestionError(String step, String detail, Throwable cause) implements AppError {

  @Override
  public String message() {
    return "Ingestion failed at step [%s]: %s".formatted(step, detail);
  }
}
