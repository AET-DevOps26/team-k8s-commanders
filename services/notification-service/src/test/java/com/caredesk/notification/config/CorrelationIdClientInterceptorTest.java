package com.caredesk.notification.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdClientInterceptorTest {

    private final CorrelationIdClientInterceptor interceptor = new CorrelationIdClientInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesCurrentCorrelationId() throws Exception {
        MDC.put(RequestLoggingFilter.MDC_KEY, "request-123");
        AtomicReference<String> observed = new AtomicReference<>();
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("http://patient-service/internal/appointments/upcoming"));

        interceptor.intercept(request, new byte[0], (forwarded, body) -> {
            observed.set(forwarded.getHeaders().getFirst(RequestLoggingFilter.HEADER_NAME));
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });

        assertThat(observed).hasValue("request-123");
    }

    @Test
    void omitsHeaderOutsideRequestContext() throws Exception {
        AtomicReference<String> observed = new AtomicReference<>();
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("http://patient-service/internal/appointments/upcoming"));

        interceptor.intercept(request, new byte[0], (forwarded, body) -> {
            observed.set(forwarded.getHeaders().getFirst(RequestLoggingFilter.HEADER_NAME));
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });

        assertThat(observed).hasValue(null);
    }
}
