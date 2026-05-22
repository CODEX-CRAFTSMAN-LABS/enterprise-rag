package com.enterprise.rag.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, Seq<String> details, Instant timestamp) {

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(code, message, List.empty(), Instant.now());
  }

  public static ErrorResponse of(String code, String message, Seq<String> details) {
    return new ErrorResponse(code, message, details, Instant.now());
  }
}
