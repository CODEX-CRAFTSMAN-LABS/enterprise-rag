package com.enterprise.rag.ingestion.domain;

public record TenantId(String value) {

  public TenantId {
    if (value == null) {
      throw new IllegalArgumentException("tenantId must not be null");
    }
  }
}
