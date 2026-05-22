package com.enterprise.rag.query.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "query")
public record QueryProperties(Cache cache) {

  public record Cache(Duration ttl) {}
}
