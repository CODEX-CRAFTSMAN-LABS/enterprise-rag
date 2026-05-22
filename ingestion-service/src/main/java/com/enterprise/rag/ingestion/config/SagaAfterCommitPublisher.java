package com.enterprise.rag.ingestion.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes Kafka saga events only after the DB transaction commits, so downstream consumers always
 * see persisted state (e.g. extracted_text before chunk step).
 */
@Component
public class SagaAfterCommitPublisher {

  public void runAfterCommit(Runnable action) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
      return;
    }
    action.run();
  }
}
