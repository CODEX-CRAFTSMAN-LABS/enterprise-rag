package com.enterprise.rag.query.adapters.in.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterprise.rag.query.domain.QueryAnswer;
import com.enterprise.rag.query.ports.in.AskQuestionUseCase;
import io.vavr.collection.List;
import io.vavr.control.Either;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@WebMvcTest(controllers = {QueryController.class, HealthController.class})
class QueryControllerWebTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AskQuestionUseCase askQuestionUseCase;

  @Test
  void ask_returnsAnswerJson_whenUseCaseSucceeds() throws Exception {
    when(askQuestionUseCase.ask(any()))
        .thenReturn(Either.right(new QueryAnswer("Enterprise RAG demo", List.empty())));

    mockMvc
        .perform(
            post("/api/v1/query/ask")
                .header("X-Tenant-Id", "acme-corp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"tenantId":"acme-corp","question":"What is this?","topK":5}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").value("Enterprise RAG demo"))
        .andExpect(jsonPath("$.citations").isArray());
  }

  @Test
  void ask_returns400_whenValidationFails() throws Exception {
    when(askQuestionUseCase.ask(any()))
        .thenReturn(
            Either.left(
                new com.enterprise.rag.common.error.ValidationError(
                    io.vavr.collection.List.of("question is required"))));

    mockMvc
        .perform(
            post("/api/v1/query/ask")
                .header("X-Tenant-Id", "acme-corp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"tenantId":"acme-corp","question":"  ","topK":5}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void health_returnsOk() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/query/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
