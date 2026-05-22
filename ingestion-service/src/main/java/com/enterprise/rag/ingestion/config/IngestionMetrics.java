package com.enterprise.rag.ingestion.config;

import com.enterprise.rag.common.observability.RagMetricNames;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IngestionMetrics {

  private final MeterRegistry meterRegistry;

  public void recordUpload(String tenantId) {
    meterRegistry.counter(RagMetricNames.INGESTION_UPLOAD_TOTAL, "tenant", tenantId).increment();
  }

  public void recordSagaStep(String step, String outcome) {
    meterRegistry
        .counter(RagMetricNames.SAGA_STEP_TOTAL, "step", step, "outcome", outcome)
        .increment();
  }
}
