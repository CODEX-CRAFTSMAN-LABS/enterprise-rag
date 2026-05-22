package com.enterprise.rag.query.config;

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
  OpenAPI queryOpenAPI() {
    SecurityScheme tenantHeader =
        new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name(TENANT_HEADER)
            .description(
                "Tenant identifier (required by platform filter; must match body.tenantId)");

    return new OpenAPI()
        .info(
            new Info()
                .title("Enterprise RAG — Query API")
                .version("v1")
                .description(
                    "CQRS read path: embed question, retrieve chunks, generate answer with citations.")
                .contact(new Contact().name("Enterprise RAG").url("https://github.com")))
        .addSecurityItem(new SecurityRequirement().addList(TENANT_HEADER))
        .components(new Components().addSecuritySchemes(TENANT_HEADER, tenantHeader));
  }
}
