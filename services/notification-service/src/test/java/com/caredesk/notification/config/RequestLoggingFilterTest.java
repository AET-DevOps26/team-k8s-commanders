package com.caredesk.notification.config;

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
        MockHttpServletRequest request = request("/notifications", "request-123");
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
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/notifications", "unsafe id"), response,
                (ignoredRequest, ignoredResponse) -> response.setStatus(401));
        assertThat(UUID.fromString(response.getHeader(RequestLoggingFilter.HEADER_NAME))).isNotNull();
    }

    @Test
    void logsServerErrorStatus() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/notifications", null), response,
                (ignoredRequest, ignoredResponse) -> response.setStatus(502));
        assertThat(response.getStatus()).isEqualTo(502);
    }

    @Test
    void logsAndRethrowsUnhandledFailure() {
        assertThatThrownBy(() -> filter.doFilter(
                request("/notifications", "request-456"),
                new MockHttpServletResponse(),
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
}
