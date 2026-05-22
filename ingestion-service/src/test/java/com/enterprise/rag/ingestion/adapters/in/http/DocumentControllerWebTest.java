package com.enterprise.rag.ingestion.adapters.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.rag.ingestion.domain.DocumentId;
import com.enterprise.rag.ingestion.ports.in.UploadDocumentUseCase;
import io.vavr.control.Either;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@WebMvcTest(controllers = {DocumentController.class, HealthController.class})
class DocumentControllerWebTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private UploadDocumentUseCase uploadDocumentUseCase;

  @Test
  void upload_returns202_whenAccepted() throws Exception {
    DocumentId id = DocumentId.generate();
    when(uploadDocumentUseCase.upload(any())).thenReturn(Either.right(id));

    MockMultipartFile file =
        new MockMultipartFile("file", "readme.txt", "text/plain", "hello rag".getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/ingestion/documents").file(file).header("X-Tenant-Id", "acme-corp"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.documentId").value(id.asString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void health_returnsOk() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/ingestion/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
