package com.enterprise.rag.ingestion.ports.out;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.ingestion.domain.Document;
import io.vavr.control.Either;

public interface TextExtractorPort {

  Either<AppError, String> extract(Document document);
}
