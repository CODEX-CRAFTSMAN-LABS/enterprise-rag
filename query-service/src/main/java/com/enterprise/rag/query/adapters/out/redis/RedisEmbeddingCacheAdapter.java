package com.enterprise.rag.query.adapters.out.redis;

import com.enterprise.rag.query.ports.out.EmbeddingCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Registered via {@link com.enterprise.rag.query.config.QueryAdapterConfig}. */
@RequiredArgsConstructor
public class RedisEmbeddingCacheAdapter implements EmbeddingCachePort {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public Option<float[]> get(String cacheKey) {
    return Option.of(redisTemplate.opsForValue().get(cacheKey))
        .flatMap(json -> Try.of(() -> objectMapper.readValue(json, float[].class)).toOption());
  }

  @Override
  public void put(String cacheKey, float[] embedding, Duration ttl) {
    Try.run(() -> redisTemplate.opsForValue().set(cacheKey, toJson(embedding), ttl))
        .onFailure(
            ex -> {
              /* cache write failures are non-fatal */
            });
  }

  private String toJson(float[] embedding) throws JsonProcessingException {
    return objectMapper.writeValueAsString(embedding);
  }
}
