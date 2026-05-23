package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.ingestion.domain.OutboxMessage;
import io.vavr.control.Either;

public interface EventPublisherPort {

  Either<AppError, Boolean> publish(OutboxMessage message);
}
