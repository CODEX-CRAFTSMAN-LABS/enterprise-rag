package com.enterprise.rag.query.domain;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;

public final class QueryRequestValidator {

  private static final int MIN_QUESTION_LEN = 3;
  private static final int MAX_QUESTION_LEN = 4000;
  private static final int MIN_TENANT_LEN = 2;
  private static final int MAX_TENANT_LEN = 64;
  private static final int MIN_TOP_K = 1;
  private static final int MAX_TOP_K = 20;
  private static final int DEFAULT_TOP_K = 5;

  private QueryRequestValidator() {}

  public static Validation<Seq<String>, QueryCommand> validate(AskRequest request) {
    return Validation.combine(
            validateTenantId(request.tenantId()),
            validateQuestion(request.question()),
            validateTopK(request.topK()))
        .ap(QueryCommand::new);
  }

  private static Validation<String, TenantId> validateTenantId(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return Validation.invalid("tenantId is required");
    }
    String trimmed = tenantId.trim();
    if (trimmed.length() < MIN_TENANT_LEN || trimmed.length() > MAX_TENANT_LEN) {
      return Validation.invalid("tenantId must be between 2 and 64 characters");
    }
    if (!trimmed.matches("[a-zA-Z0-9_-]+")) {
      return Validation.invalid("tenantId must be alphanumeric with _ or -");
    }
    return Validation.valid(new TenantId(trimmed));
  }

  private static Validation<String, String> validateQuestion(String question) {
    if (question == null || question.isBlank()) {
      return Validation.invalid("question is required");
    }
    String trimmed = question.trim();
    if (trimmed.length() < MIN_QUESTION_LEN || trimmed.length() > MAX_QUESTION_LEN) {
      return Validation.invalid("question must be between 3 and 4000 characters");
    }
    return Validation.valid(trimmed);
  }

  private static Validation<String, Integer> validateTopK(Integer topK) {
    int value = topK == null ? DEFAULT_TOP_K : topK;
    if (value < MIN_TOP_K || value > MAX_TOP_K) {
      return Validation.invalid("topK must be between 1 and 20");
    }
    return Validation.valid(value);
  }
}
