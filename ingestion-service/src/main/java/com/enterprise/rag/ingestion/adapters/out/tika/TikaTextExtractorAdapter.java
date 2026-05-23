package com.enterprise.rag.ingestion.adapters.out.tika;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.IngestionError;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.ports.out.TextExtractorPort;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TikaTextExtractorAdapter implements TextExtractorPort {

  private final Tika tika;

  @Override
  public Either<AppError, String> extract(Document document) {
    return Try.of(() -> tika.parseToString(new ByteArrayInputStream(document.content())))
        .toEither()
        .map(String::trim)
        .mapLeft(t -> (AppError) new IngestionError("parse", t.getMessage(), t))
        .flatMap(
            text -> {
              if (text.isBlank()) {
                return Either.left(
                    (AppError)
                        new IngestionError("parse", "no text extracted from document", null));
              }
              return Either.right(text);
            });
  }
}
