package com.caredesk.auth.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesValidCorrelationIdAndScopesItToRequest() throws Exception {
        MockHttpServletRequest request = request("/auth/login", "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedId = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            observedId.set(MDC.get(RequestLoggingFilter.MDC_KEY));
            response.setStatus(204);
        });

        assertThat(observedId).hasValue("request-123");
        assertThat(response.getHeader(RequestLoggingFilter.HEADER_NAME)).isEqualTo("request-123");
        assertThat(MDC.get(RequestLoggingFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeCorrelationIdAndLogsClientError() throws Exception {
        MockHttpServletRequest request = request("/auth/login", "unsafe id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> response.setStatus(400));

        assertThatCodeIsUuid(response.getHeader(RequestLoggingFilter.HEADER_NAME));
    }

    @Test
    void logsServerErrorStatus() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/auth/login", null), response,
                (ignoredRequest, ignoredResponse) -> response.setStatus(503));
        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void logsAndRethrowsUnhandledFailure() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request("/auth/login", "request-456"), response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("boom");
                })).isInstanceOf(ServletException.class);
        assertThat(MDC.get(RequestLoggingFilter.MDC_KEY)).isNull();
    }

    private static MockHttpServletRequest request(String path, String correlationId) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (correlationId != null) {
            request.addHeader(RequestLoggingFilter.HEADER_NAME, correlationId);
        }
        return request;
    }

    private static void assertThatCodeIsUuid(String value) {
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
