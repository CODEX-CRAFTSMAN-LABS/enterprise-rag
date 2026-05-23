package com.enterprise.rag.query.ports.out;

import com.enterprise.rag.common.error.AppError;
import io.vavr.control.Either;

public interface EmbeddingPort {

  Either<AppError, float[]> embed(String text);
}
