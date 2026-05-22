package com.enterprise.rag.ingestion.application;

/** Thrown when a saga step should be retried (e.g. DB commit not yet visible to consumers). */
public class TransientIngestionException extends RuntimeException {

  public TransientIngestionException(String message) {
    super(message);
  }
}
