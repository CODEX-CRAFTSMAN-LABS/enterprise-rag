package com.enterprise.rag.query.config;

import com.enterprise.rag.common.observability.RagMetricNames;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueryMetrics {

  private final MeterRegistry meterRegistry;

  public void recordAsk(String tenantId, long durationMs, boolean success) {
    meterRegistry
        .counter(
            RagMetricNames.QUERY_ASK_TOTAL, "tenant", tenantId, "success", String.valueOf(success))
        .increment();
    Timer.builder("rag.query.ask.duration")
        .tag("tenant", tenantId)
        .register(meterRegistry)
        .record(durationMs, TimeUnit.MILLISECONDS);
  }
}
