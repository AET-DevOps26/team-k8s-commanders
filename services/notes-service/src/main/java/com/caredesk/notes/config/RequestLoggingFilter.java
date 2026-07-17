package com.caredesk.notes.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Adds request correlation and one completion log entry for every HTTP request. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = correlationId(request.getHeader(HEADER_NAME));
        response.setHeader(HEADER_NAME, correlationId);
        long startedAt = System.nanoTime();

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException error) {
                log.error("request failed method={} path={} durationMs={} exception={}",
                        request.getMethod(), request.getRequestURI(), elapsedMillis(startedAt),
                        error.getClass().getSimpleName());
                throw error;
            }
            logCompletion(request, response.getStatus(), startedAt);
        }
    }

    static String correlationId(String candidate) {
        return candidate != null && VALID_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }

    private static void logCompletion(HttpServletRequest request, int status, long startedAt) {
        String message = "request completed method={} path={} status={} durationMs={}";
        Object[] arguments = {request.getMethod(), request.getRequestURI(), status, elapsedMillis(startedAt)};
        if (status >= 500) {
            log.error(message, arguments);
        } else if (status >= 400) {
            log.warn(message, arguments);
        } else {
            log.info(message, arguments);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
