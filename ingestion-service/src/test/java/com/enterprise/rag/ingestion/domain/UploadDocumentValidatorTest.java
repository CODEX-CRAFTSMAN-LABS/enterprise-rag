package com.enterprise.rag.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UploadDocumentValidatorTest {

  @Test
  void should_accumulateAllValidationErrors_when_multipleFieldsInvalid() {
    UploadCommand command =
        new UploadCommand(new TenantId("x"), "", "application/pdf", new byte[0]);

    var validation = UploadDocumentValidator.validate(command);

    assertThat(validation.isInvalid()).isTrue();
    assertThat(validation.getError().size()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void should_returnValidCommand_when_allFieldsOk() {
    UploadCommand command =
        new UploadCommand(
            new TenantId("tenant-1"), "report.pdf", "application/pdf", "data".getBytes());

    var validation = UploadDocumentValidator.validate(command);

    assertThat(validation.isValid()).isTrue();
    assertThat(validation.get().filename()).isEqualTo("report.pdf");
  }
}
