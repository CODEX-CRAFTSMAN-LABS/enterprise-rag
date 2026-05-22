package com.enterprise.rag.query.adapters.in.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RAG question request")
public record AskRequestDto(
    @Schema(example = "acme-corp", description = "Must match X-Tenant-Id header") String tenantId,
    @Schema(example = "What is this project about?") String question,
    @Schema(example = "5", description = "Number of chunks to retrieve; default applied if null")
        Integer topK) {}
