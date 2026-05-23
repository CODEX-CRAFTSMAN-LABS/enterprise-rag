package com.enterprise.rag.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterprise.rag.common.error.CacheMiss;
import com.enterprise.rag.common.error.EmbeddingError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.common.error.LlmError;
import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.common.error.RateLimitError;
import com.enterprise.rag.common.error.RetrievalError;
import com.enterprise.rag.common.error.TenantError;
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

  @Test
  void should_mapErrorStatuses_forToError() {
    assertThat(ResponseMapper.toError(new EmbeddingError("e", null)).getStatusCode())
        .isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(ResponseMapper.toError(new LlmError("l", null)).getStatusCode())
        .isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(ResponseMapper.toError(new RetrievalError("r", null)).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(ResponseMapper.toError(new IngestionError("i", "d", null)).getStatusCode())
        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(ResponseMapper.toError(new CacheMiss("key")).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(ResponseMapper.toError(new RateLimitError("t", 30)).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(ResponseMapper.toError(new TenantError("bad tenant")).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void should_mapErrorBodies_forToErrorResponse() {
    assertThat(ResponseMapper.toErrorResponse(new EmbeddingError("embed", null)).getBody().code())
        .isEqualTo("EMBEDDING_ERROR");
    assertThat(ResponseMapper.toErrorResponse(new LlmError("llm", null)).getBody().code())
        .isEqualTo("LLM_ERROR");
    assertThat(ResponseMapper.toErrorResponse(new RetrievalError("ret", null)).getBody().code())
        .isEqualTo("RETRIEVAL_ERROR");
    assertThat(
            ResponseMapper.toErrorResponse(new IngestionError("step", "detail", null))
                .getBody()
                .code())
        .isEqualTo("INGESTION_ERROR");
    assertThat(ResponseMapper.toErrorResponse(new CacheMiss("k")).getBody().code())
        .isEqualTo("CACHE_MISS");
    var rateLimited = ResponseMapper.toErrorResponse(new RateLimitError("t1", 45));
    assertThat(rateLimited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(rateLimited.getHeaders().getFirst("Retry-After")).isEqualTo("45");
    assertThat(ResponseMapper.toErrorResponse(new TenantError("missing")).getBody().code())
        .isEqualTo("TENANT_ERROR");
  }
}
