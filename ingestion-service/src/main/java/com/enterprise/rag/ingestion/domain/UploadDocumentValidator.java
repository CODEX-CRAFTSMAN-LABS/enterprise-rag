package com.enterprise.rag.ingestion.domain;

import io.vavr.collection.Seq;
import io.vavr.control.Validation;

/** Pure validation — accumulates all violations (no fail-fast). */
public final class UploadDocumentValidator {

  private static final long MAX_BYTES = 52_428_800L; // 50 MiB
  private static final int MIN_TENANT_LEN = 2;
  private static final int MAX_TENANT_LEN = 64;

  private UploadDocumentValidator() {}

  public static Validation<Seq<String>, UploadCommand> validate(UploadCommand command) {
    return Validation.combine(
            validateTenantId(command.tenantId()),
            validateFilename(command.filename()),
            validateContentType(command.contentType()),
            validateContent(command.content()))
        .ap(
            (tenant, filename, contentType, content) ->
                new UploadCommand(tenant, filename, contentType, content));
  }

  private static Validation<String, TenantId> validateTenantId(TenantId tenantId) {
    String raw = tenantId.value().trim();
    if (raw.length() < MIN_TENANT_LEN || raw.length() > MAX_TENANT_LEN) {
      return Validation.invalid(
          "tenantId must be between %d and %d characters"
              .formatted(MIN_TENANT_LEN, MAX_TENANT_LEN));
    }
    if (!raw.matches("[a-zA-Z0-9_-]+")) {
      return Validation.invalid("tenantId must be alphanumeric with _ or -");
    }
    return Validation.valid(new TenantId(raw));
  }

  private static Validation<String, String> validateFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return Validation.invalid("filename is required");
    }
    if (filename.length() > 512) {
      return Validation.invalid("filename must not exceed 512 characters");
    }
    return Validation.valid(filename.trim());
  }

  private static Validation<String, String> validateContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return Validation.invalid("contentType is required");
    }
    if (contentType.length() > 128) {
      return Validation.invalid("contentType must not exceed 128 characters");
    }
    return Validation.valid(contentType.trim());
  }

  private static Validation<String, byte[]> validateContent(byte[] content) {
    if (content == null || content.length == 0) {
      return Validation.invalid("file content must not be empty");
    }
    if (content.length > MAX_BYTES) {
      return Validation.invalid("file size exceeds maximum of 50 MiB");
    }
    return Validation.valid(content);
  }
}
