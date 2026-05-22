package com.enterprise.rag.ingestion.adapters.in.http;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.ValidationError;
import com.enterprise.rag.common.web.ResponseMapper;
import com.enterprise.rag.ingestion.adapters.in.http.dto.DocumentUploadResponse;
import com.enterprise.rag.ingestion.config.OpenApiConfig;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.UploadCommand;
import com.enterprise.rag.ingestion.ports.in.UploadDocumentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Upload documents for async ingestion")
@SecurityRequirement(name = OpenApiConfig.TENANT_HEADER)
public class DocumentController {

  private final UploadDocumentUseCase uploadDocumentUseCase;

  @Operation(
      summary = "Upload a document",
      description =
          "Accepts multipart file upload. Returns 202 with document id; processing continues via Kafka saga.")
  @ApiResponses({
    @ApiResponse(responseCode = "202", description = "Accepted for ingestion"),
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded", content = @Content)
  })
  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadDocument(
      @Parameter(description = "Tenant id", required = true) @RequestHeader("X-Tenant-Id")
          String tenantId,
      @Parameter(description = "Document file (e.g. PDF, TXT, MD; max 50MB)", required = true)
          @RequestPart("file")
          MultipartFile file) {

    return Try.of(
            () ->
                new UploadCommand(
                    new TenantId(tenantId),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()))
        .toEither()
        .mapLeft(
            t ->
                (AppError) new ValidationError(List.of("failed to read upload: " + t.getMessage())))
        .flatMap(uploadDocumentUseCase::upload)
        .fold(
            ResponseMapper::toErrorResponse,
            id -> ResponseEntity.accepted().body(DocumentUploadResponse.from(id)));
  }
}
