package com.caredesk.notification.config;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/** Propagates current request correlation id to internal service calls. */
public final class CorrelationIdClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get(RequestLoggingFilter.MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(RequestLoggingFilter.HEADER_NAME, correlationId);
        }
        return execution.execute(request, body);
    }
}
