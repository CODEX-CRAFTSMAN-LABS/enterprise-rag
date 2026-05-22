package com.enterprise.rag.common.platform;

import com.enterprise.rag.common.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Enforces tenant header + per-tenant rate limit on API routes. System design pattern: Rate
 * limiting + multi-tenant guard
 */
public class TenantRateLimitFilter implements Filter {

  private final TenantRateLimiter rateLimiter;
  private final PlatformProperties properties;
  private final ObjectMapper objectMapper;

  public TenantRateLimitFilter(
      TenantRateLimiter rateLimiter, PlatformProperties properties, ObjectMapper objectMapper) {
    this.rateLimiter = rateLimiter;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest httpRequest)
        || !(response instanceof HttpServletResponse httpResponse)) {
      chain.doFilter(request, response);
      return;
    }

    String path = httpRequest.getRequestURI();
    if (isExemptPath(path)) {
      chain.doFilter(request, response);
      return;
    }

    String tenantId = httpRequest.getHeader(properties.tenantHeaderName());

    if (properties.tenantHeaderRequired() && (tenantId == null || tenantId.isBlank())) {
      writeError(httpResponse, HttpStatus.BAD_REQUEST, "TENANT_REQUIRED", "Missing tenant header");
      return;
    }

    if (properties.rateLimitEnabled() && tenantId != null && !tenantId.isBlank()) {
      if (!rateLimiter.tryConsume(tenantId.trim())) {
        httpResponse.setHeader("Retry-After", "60");
        writeError(
            httpResponse,
            HttpStatus.TOO_MANY_REQUESTS,
            "RATE_LIMIT_EXCEEDED",
            "Rate limit exceeded for tenant: " + tenantId);
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private static boolean isExemptPath(String path) {
    return path.startsWith("/actuator")
        || path.contains("/health")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.equals("/swagger-ui.html");
  }

  private void writeError(
      HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(code, message));
  }
}
