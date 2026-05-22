package com.enterprise.rag.query.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QueryRequestValidatorTest {

  @Test
  void should_accumulateErrors_when_multipleFieldsInvalid() {
    var validation = QueryRequestValidator.validate(new AskRequest("x", "", null));
    assertThat(validation.isInvalid()).isTrue();
    assertThat(validation.getError().size()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void should_defaultTopK_when_notProvided() {
    var validation =
        QueryRequestValidator.validate(
            new AskRequest("acme-corp", "What is our refund policy?", null));
    assertThat(validation.isValid()).isTrue();
    assertThat(validation.get().topK()).isEqualTo(5);
  }
}
