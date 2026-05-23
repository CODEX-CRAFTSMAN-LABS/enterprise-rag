package com.enterprise.rag.query.adapters.out.redis;

import com.enterprise.rag.query.ports.out.EmbeddingCachePort;
import io.vavr.control.Option;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** Fallback when Redis is unavailable (local profile or missing StringRedisTemplate). */
public class InMemoryEmbeddingCacheAdapter implements EmbeddingCachePort {

  private final ConcurrentHashMap<String, float[]> cache = new ConcurrentHashMap<>();

  @Override
  public Option<float[]> get(String cacheKey) {
    return Option.of(cache.get(cacheKey));
  }

  @Override
  public void put(String cacheKey, float[] embedding, Duration ttl) {
    cache.put(cacheKey, embedding);
  }
}
