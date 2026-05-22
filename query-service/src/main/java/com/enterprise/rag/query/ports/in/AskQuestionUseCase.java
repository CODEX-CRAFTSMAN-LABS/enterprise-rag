package com.enterprise.rag.query.ports.in;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.query.domain.AskRequest;
import com.enterprise.rag.query.domain.QueryAnswer;
import io.vavr.control.Either;

public interface AskQuestionUseCase {

  Either<AppError, QueryAnswer> ask(AskRequest request);
}
