package com.enterprise.rag.query.ports.out;

import com.enterprise.rag.common.error.AppError;
import io.vavr.control.Either;

public interface LlmPort {

  Either<AppError, String> complete(String prompt);
}
