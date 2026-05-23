package com.enterprise.rag.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.rag.common.error.NotFoundError;
import com.enterprise.rag.ingestion.domain.Document;
import com.enterprise.rag.ingestion.domain.DocumentChunkedEvent;
import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.domain.DocumentStatus;
import com.enterprise.rag.ingestion.domain.TenantId;
import com.enterprise.rag.ingestion.domain.TextChunk;
import com.enterprise.rag.ingestion.ports.out.ChunkRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.DocumentRepositoryPort;
import com.enterprise.rag.ingestion.ports.out.VectorIndexPort;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbedIndexDocumentServiceTest {

  @Mock private DocumentRepositoryPort documentRepository;
  @Mock private ChunkRepositoryPort chunkRepository;
  @Mock private VectorIndexPort vectorIndexPort;
  @Mock private IngestionFailureHandler failureHandler;

  private EmbedIndexDocumentService service;
  private DocumentId documentId;
  private TenantId tenantId;

  @BeforeEach
  void setUp() {
    service =
        new EmbedIndexDocumentService(
            documentRepository, chunkRepository, vectorIndexPort, failureHandler);
    documentId = DocumentId.generate();
    tenantId = new TenantId("acme-corp");
  }

  @Test
  void should_returnNotFound_when_documentMissing() {
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.none());

    var result = service.handle(chunkedEvent());

    assertThat(result.isLeft()).isTrue();
    assertThat(result.getLeft()).isInstanceOf(NotFoundError.class);
    verify(failureHandler).fail(eq(documentId), eq(tenantId), eq("embed-index"), any());
  }

  @Test
  void should_skipIndex_when_alreadyIndexed() {
    Document indexed =
        TestDocuments.document(documentId, tenantId, DocumentStatus.INDEXED, Option.of("text"));
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(indexed));

    var result = service.handle(chunkedEvent());

    assertThat(result.isRight()).isTrue();
    verify(vectorIndexPort, never()).index(any(), any(), any());
  }

  @Test
  void should_indexAndMarkIndexed_when_chunksExist() {
    Document processing =
        TestDocuments.document(
            documentId, tenantId, DocumentStatus.PROCESSING, Option.of("chunked text"));
    when(documentRepository.findById(documentId, tenantId)).thenReturn(Option.of(processing));
    List<TextChunk> chunks = List.of(new TextChunk(0, "chunk one"));
    when(chunkRepository.findByDocument(documentId, tenantId)).thenReturn(chunks);
    when(vectorIndexPort.index(documentId, tenantId, chunks)).thenReturn(Either.right(1));

    var result = service.handle(chunkedEvent());

    assertThat(result.isRight()).isTrue();
    verify(documentRepository).updateStatus(documentId, tenantId, DocumentStatus.INDEXED);
  }

  private DocumentChunkedEvent chunkedEvent() {
    return new DocumentChunkedEvent("evt-3", documentId.asString(), tenantId.value(), 2);
  }
}
