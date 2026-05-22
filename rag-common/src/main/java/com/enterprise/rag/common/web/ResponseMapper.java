package com.enterprise.rag.common.web;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.CacheMiss;
import com.enterprise.rag.common.error.EmbeddingError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.common.error.LlmError;
import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.common.error.RateLimitError;
import com.enterprise.rag.common.error.RetrievalError;
import com.enterprise.rag.common.error.TenantError;
import com.enterprise.rag.common.error.ValidationError;
import io.vavr.control.Either;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Maps functional {@link Either} results to HTTP — shared across query and ingestion APIs. */
public final class ResponseMapper {

  private ResponseMapper() {}

  public static <T> ResponseEntity<T> toResponse(Either<AppError, T> result) {
    return result.fold(ResponseMapper::toError, ResponseEntity::ok);
  }

  @SuppressWarnings("unchecked")
  public static <T> ResponseEntity<T> toError(AppError error) {
    if (error instanceof NotFoundError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    if (error instanceof ValidationError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    if (error instanceof EmbeddingError || error instanceof LlmError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }
    if (error instanceof RetrievalError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
    if (error instanceof IngestionError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    }
    if (error instanceof CacheMiss) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    if (error instanceof RateLimitError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    if (error instanceof TenantError) {
      return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    return (ResponseEntity<T>) ResponseEntity.internalServerError().build();
  }

  public static ResponseEntity<ErrorResponse> toErrorResponse(AppError error) {
    if (error instanceof NotFoundError e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ErrorResponse.of("NOT_FOUND", e.message()));
    }
    if (error instanceof ValidationError e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ErrorResponse.of("VALIDATION_ERROR", e.message(), e.violations()));
    }
    if (error instanceof EmbeddingError e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(ErrorResponse.of("EMBEDDING_ERROR", e.message()));
    }
    if (error instanceof LlmError e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(ErrorResponse.of("LLM_ERROR", e.message()));
    }
    if (error instanceof RetrievalError e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(ErrorResponse.of("RETRIEVAL_ERROR", e.message()));
    }
    if (error instanceof IngestionError e) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
          .body(ErrorResponse.of("INGESTION_ERROR", e.message()));
    }
    if (error instanceof CacheMiss e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(ErrorResponse.of("CACHE_MISS", e.message()));
    }
    if (error instanceof RateLimitError e) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header("Retry-After", String.valueOf(e.retryAfterSeconds()))
          .body(ErrorResponse.of("RATE_LIMIT_EXCEEDED", e.message()));
    }
    if (error instanceof TenantError e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(ErrorResponse.of("TENANT_ERROR", e.message()));
    }
    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of("INTERNAL_ERROR", error.message()));
  }
}
