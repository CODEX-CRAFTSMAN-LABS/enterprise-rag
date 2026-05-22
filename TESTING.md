# Testing guide — Enterprise RAG

## What exists today

| Layer | Count | Examples |
|-------|-------|----------|
| **Unit** | 8 classes | `AskQuestionServiceTest`, `UploadDocumentServiceTest`, validators, chunker |
| **Integration (web)** | 2 classes | `QueryControllerWebTest`, `DocumentControllerWebTest` |
| **E2E / Testcontainers** | 0 | Not yet (Postgres/Kafka/Ollama full stack) |

Unit tests use **JUnit 5 + Mockito**. Integration tests use **`@WebMvcTest`** (Spring MVC + JSON/multipart, mocked use cases).

---

## 1. Run tests locally

```bash
./gradlew spotlessApply   # format first if CI fails on spotlessCheck
./gradlew test            # everything (same as GitHub Actions)
./gradlew unitTest        # unit only
./gradlew integrationTest # HTTP API slice only
```

Reports:

```bash
open rag-common/build/reports/tests/test/index.html
open query-service/build/reports/tests/test/index.html
open ingestion-service/build/reports/tests/test/index.html
```

Coverage:

```bash
./gradlew test jacocoAggregatedReport
open build/reports/jacoco/aggregated/html/index.html
```

---

## 2. Verify “everything works” (checklist)

### Code quality

```bash
./gradlew spotlessCheck test jacocoAggregatedReport coverageCheck
```

### Docker stack (full RAG)

```bash
./scripts/docker-up.sh
curl -s http://localhost:8081/api/v1/ingestion/health
curl -s http://localhost:8082/api/v1/query/health
# Upload + ask — see README or postman/
```

### Kubernetes (optional)

```bash
kubectl apply -k k8s/overlays/dev
kubectl get pods -n enterprise-rag-dev
```

### CI (GitHub Actions)

Push any branch → **Actions** tab → workflow **CI** must pass (Spotless, build, test, Docker build).

---

## 3. What is *not* covered yet

- Full **Kafka saga** integration (upload → parse → chunk → index)
- **Postgres/pgvector** retrieval with real embeddings
- **Ollama** LLM calls in tests
- **REST** tests with real `AskQuestionService` wiring (only controller slice today)

Those need Testcontainers or a test profile with embedded services (future work).

---

## 4. Add a new unit test

Place under `src/test/java/...` in the right module. Use `@ExtendWith(MockitoExtension.class)` and mock ports (see `AskQuestionServiceTest`).

## 5. Add a new integration test

Use `@Tag("integration")` + `@WebMvcTest(controllers = YourController.class)` + `@MockBean` for use cases (see `QueryControllerWebTest`).
