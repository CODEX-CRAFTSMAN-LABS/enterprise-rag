package com.enterprise.rag.query.adapters.in.http;

import com.enterprise.rag.common.web.ResponseMapper;
import com.enterprise.rag.query.adapters.in.http.dto.AskRequestDto;
import com.enterprise.rag.query.adapters.in.http.dto.AskResponseDto;
import com.enterprise.rag.query.config.OpenApiConfig;
import com.enterprise.rag.query.domain.AskRequest;
import com.enterprise.rag.query.ports.in.AskQuestionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
@Tag(name = "Query", description = "RAG question answering")
@SecurityRequirement(name = OpenApiConfig.TENANT_HEADER)
public class QueryController {

  private final AskQuestionUseCase askQuestionUseCase;

  @Operation(
      summary = "Ask a question",
      description = "Retrieves relevant chunks and generates an answer with citations.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Answer with citations"),
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content),
    @ApiResponse(
        responseCode = "503",
        description = "Embedding or LLM circuit open",
        content = @Content)
  })
  @PostMapping("/ask")
  public ResponseEntity<?> ask(
      @Parameter(description = "Tenant id (required by platform filter)", required = true)
          @RequestHeader("X-Tenant-Id")
          String tenantHeader,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Question and retrieval options",
              required = true,
              content = @Content(schema = @Schema(implementation = AskRequestDto.class)))
          @RequestBody
          AskRequestDto body) {
    AskRequest request = new AskRequest(body.tenantId(), body.question(), body.topK());
    return askQuestionUseCase
        .ask(request)
        .fold(
            ResponseMapper::toErrorResponse,
            answer -> ResponseEntity.ok(AskResponseDto.from(answer)));
  }
}
