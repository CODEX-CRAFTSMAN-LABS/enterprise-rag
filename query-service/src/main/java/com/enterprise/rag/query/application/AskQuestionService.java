package com.enterprise.rag.query.application;

import com.enterprise.rag.common.error.AppError;
import com.enterprise.rag.common.error.ValidationError;
import com.enterprise.rag.query.config.QueryMetrics;
import com.enterprise.rag.query.config.QueryProperties;
import com.enterprise.rag.query.domain.AskRequest;
import com.enterprise.rag.query.domain.QueryAnswer;
import com.enterprise.rag.query.domain.QueryAnswer.Citation;
import com.enterprise.rag.query.domain.QueryCommand;
import com.enterprise.rag.query.domain.QueryRequestValidator;
import com.enterprise.rag.query.domain.RagPromptBuilder;
import com.enterprise.rag.query.domain.RetrievedChunk;
import com.enterprise.rag.query.ports.in.AskQuestionUseCase;
import com.enterprise.rag.query.ports.out.EmbeddingCachePort;
import com.enterprise.rag.query.ports.out.EmbeddingPort;
import com.enterprise.rag.query.ports.out.LlmPort;
import com.enterprise.rag.query.ports.out.RetrievalPort;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// System design pattern: Cache-aside + Circuit breaker + CQRS read path
// Embeddings cached in Redis; retrieval uses pgvector; LLM wrapped with Resilience4j.

@Service
@RequiredArgsConstructor
public class AskQuestionService implements AskQuestionUseCase {

  private final EmbeddingCachePort embeddingCachePort;
  private final EmbeddingPort embeddingPort;
  private final RetrievalPort retrievalPort;
  private final LlmPort llmPort;
  private final QueryProperties queryProperties;
  private final QueryMetrics queryMetrics;

  @Override
  public Either<AppError, QueryAnswer> ask(AskRequest request) {
    long start = System.currentTimeMillis();
    Either<AppError, QueryAnswer> result =
        QueryRequestValidator.validate(request)
            .toEither()
            .mapLeft(errors -> (AppError) new ValidationError(errors))
            .flatMap(this::executeRag);
    String tenant = request.tenantId() == null ? "unknown" : request.tenantId().trim();
    queryMetrics.recordAsk(tenant, System.currentTimeMillis() - start, result.isRight());
    return result;
  }

  private Either<AppError, QueryAnswer> executeRag(QueryCommand command) {
    return getEmbedding(command)
        .flatMap(embedding -> retrievalPort.retrieve(command.tenantId(), embedding, command.topK()))
        .flatMap(chunks -> generateAnswer(command, chunks));
  }

  private Either<AppError, float[]> getEmbedding(QueryCommand command) {
    String cacheKey =
        EmbeddingCacheKeys.forQuestion(command.tenantId().value(), command.question());
    Option<float[]> cached = embeddingCachePort.get(cacheKey);
    if (cached.isDefined()) {
      return Either.right(cached.get());
    }
    return embeddingPort
        .embed(command.question())
        .peek(
            embedding ->
                embeddingCachePort.put(cacheKey, embedding, queryProperties.cache().ttl()));
  }

  private Either<AppError, QueryAnswer> generateAnswer(
      QueryCommand command, List<RetrievedChunk> chunks) {
    if (chunks.isEmpty()) {
      return Either.right(
          new QueryAnswer(
              "I do not have enough indexed context to answer that question yet.", List.empty()));
    }
    String prompt = RagPromptBuilder.build(command.question(), chunks);
    return llmPort
        .complete(prompt)
        .map(answer -> new QueryAnswer(answer, chunks.map(Citation::from)));
  }
}
