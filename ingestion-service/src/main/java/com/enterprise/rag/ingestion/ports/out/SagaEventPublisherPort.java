package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentIngestionFailedEvent;
import com.enterprise.rag.ingestion.domain.DocumentParsedEvent;
import io.vavr.control.Either;

public interface SagaEventPublisherPort {

  Either<AppError, Boolean> publishParsed(DocumentParsedEvent event);

  Either<AppError, Boolean> publishChunked(DocumentChunkedEvent event);

  Either<AppError, Boolean> publishFailed(DocumentIngestionFailedEvent event);
}
