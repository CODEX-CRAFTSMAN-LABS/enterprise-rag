package com.enterprise.rag.query.ports.out;

import io.vavr.control.Option;
import java.time.Duration;

public interface EmbeddingCachePort {

  Option<float[]> get(String cacheKey);

  void put(String cacheKey, float[] embedding, Duration ttl);
}
