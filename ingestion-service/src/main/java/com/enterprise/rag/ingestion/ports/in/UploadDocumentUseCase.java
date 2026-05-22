package com.enterprise.rag.ingestion.ports.in;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.UploadCommand;
import io.vavr.control.Either;

public interface UploadDocumentUseCase {

  Either<AppError, DocumentId> upload(UploadCommand command);
}
