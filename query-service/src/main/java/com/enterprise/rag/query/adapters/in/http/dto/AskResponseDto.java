package com.enterprise.rag.query.adapters.in.http.dto;

import com.enterprise.rag.query.domain.QueryAnswer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generated answer with source citations")
public record AskResponseDto(String answer, List<CitationDto> citations) {

  @Schema(description = "Retrieved chunk used in the answer")
  public record CitationDto(String documentId, int chunkIndex, double score) {}

  public static AskResponseDto from(QueryAnswer answer) {
    return new AskResponseDto(
        answer.answer(),
        answer
            .citations()
            .map(c -> new CitationDto(c.documentId(), c.chunkIndex(), c.score()))
            .toJavaList());
  }
}
