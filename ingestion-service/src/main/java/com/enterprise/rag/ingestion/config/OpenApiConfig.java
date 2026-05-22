package com.enterprise.rag.ingestion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  public static final String TENANT_HEADER = "X-Tenant-Id";

  @Bean
  OpenAPI ingestionOpenAPI() {
    SecurityScheme tenantHeader =
        new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name(TENANT_HEADER)
            .description("Tenant identifier for multi-tenant isolation and rate limiting");

    return new OpenAPI()
        .info(
            new Info()
                .title("Enterprise RAG — Ingestion API")
                .version("v1")
                .description("CQRS write path: upload documents, trigger ingestion saga.")
                .contact(new Contact().name("Enterprise RAG").url("https://github.com")))
        .addSecurityItem(new SecurityRequirement().addList(TENANT_HEADER))
        .components(new Components().addSecuritySchemes(TENANT_HEADER, tenantHeader));
  }
}
