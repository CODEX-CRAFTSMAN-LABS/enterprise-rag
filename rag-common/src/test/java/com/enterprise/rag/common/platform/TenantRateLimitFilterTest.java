package com.enterprise.rag.common.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class TenantRateLimitFilterTest {

  @Mock private TenantRateLimiter rateLimiter;
  @Mock private FilterChain filterChain;

  private TenantRateLimitFilter filter;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    filter = new TenantRateLimitFilter(rateLimiter, PlatformProperties.defaults(), objectMapper);
  }

  @Test
  void should_allowActuatorWithoutTenant() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void should_return400_when_tenantHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/query");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentAsString()).contains("TENANT_REQUIRED");
  }

  @Test
  void should_return429_when_rateLimitExceeded() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/query");
    request.addHeader("X-Tenant-Id", "acme-corp");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(rateLimiter.tryConsume("acme-corp")).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    verify(filterChain, never()).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("60");
  }

  @Test
  void should_continueChain_when_tenantAllowed() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/query");
    request.addHeader("X-Tenant-Id", "acme-corp");
    MockHttpServletResponse response = new MockHttpServletResponse();
    when(rateLimiter.tryConsume("acme-corp")).thenReturn(true);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }
}
