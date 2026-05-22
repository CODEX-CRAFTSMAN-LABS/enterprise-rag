package com.enterprise.rag.common.error;

public record TenantError(String detail) implements AppError {

  @Override
  public String message() {
    return "Tenant error: " + detail;
  }
}
