package com.enterprise.rag.query.adapters.out.vector;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.RetrievalError;
import com.enterprise.rag.query.domain.RetrievedChunk;
import com.enterprise.rag.query.domain.TenantId;
import com.enterprise.rag.query.ports.out.RetrievalPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/** Registered via {@link com.enterprise.rag.query.config.QueryAdapterConfig}. */
@RequiredArgsConstructor
public class PgvectorRetrievalAdapter implements RetrievalPort {

  private static final String SQL =
      """
      SELECT content, metadata, (embedding <=> ?::vector) AS distance
      FROM vector_store
      WHERE metadata::jsonb ->> 'tenantId' = ?
      ORDER BY embedding <=> ?::vector
      LIMIT ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public Either<AppError, List<RetrievedChunk>> retrieve(
      TenantId tenantId, float[] queryEmbedding, int topK) {
    String vectorLiteral = toVectorLiteral(queryEmbedding);
    return Try.of(
            () ->
                List.ofAll(
                    jdbcTemplate.query(
                        SQL,
                        (rs, rowNum) -> mapRow(rs),
                        vectorLiteral,
                        tenantId.value(),
                        vectorLiteral,
                        topK)))
        .toEither()
        .mapLeft(t -> (AppError) new RetrievalError(t.getMessage(), t));
  }

  private RetrievedChunk mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
    try {
      JsonNode metadata = objectMapper.readTree(rs.getString("metadata"));
      double distance = rs.getDouble("distance");
      return new RetrievedChunk(
          metadata.path("documentId").asText("unknown"),
          metadata.path("chunkIndex").asInt(0),
          rs.getString("content"),
          1.0 - distance);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid vector metadata JSON", e);
    }
  }

  private String toVectorLiteral(float[] embedding) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < embedding.length; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(embedding[i]);
    }
    builder.append(']');
    return builder.toString();
  }
}
