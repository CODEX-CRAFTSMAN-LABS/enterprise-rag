package com.enterprise.rag.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.common.error.ValidationError;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ResponseMapperTest {

  @Test
  void should_return200_when_eitherIsRight() {
    var response = ResponseMapper.toResponse(Either.right("ok"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("ok");
  }

  @Test
  void should_return404_when_notFoundError() {
    var response = ResponseMapper.toErrorResponse(new NotFoundError("Document", "doc-1"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
  }

  @Test
  void should_return400_withDetails_when_validationError() {
    var response =
        ResponseMapper.toErrorResponse(new ValidationError(List.of("question required")));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().details()).containsExactly("question required");
  }
}
