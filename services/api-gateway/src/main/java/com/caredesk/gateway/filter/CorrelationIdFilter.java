package com.caredesk.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Establishes one safe correlation id at the edge and forwards it downstream.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Correlation-ID";
    private static final int MAX_ID_LENGTH = 128;
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = correlationId(exchange.getRequest().getHeaders().getFirst(HEADER_NAME));
        long startedAt = System.nanoTime();

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.set(HEADER_NAME, correlationId))
                .build();
        ServerWebExchange correlatedExchange = exchange.mutate().request(request).build();
        correlatedExchange.getResponse().getHeaders().set(HEADER_NAME, correlationId);

        return chain.filter(correlatedExchange)
                .doOnSuccess(ignored -> logCompletion(correlatedExchange, correlationId, startedAt))
                .doOnError(error -> log.error(
                        "request failed correlationId={} method={} path={} durationMs={} exception={}",
                        correlationId,
                        request.getMethod(),
                        request.getURI().getPath(),
                        elapsedMillis(startedAt),
                        error.getClass().getSimpleName()));
    }

    static String correlationId(String candidate) {
        if (candidate != null
                && candidate.length() <= MAX_ID_LENGTH
                && VALID_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private static void logCompletion(ServerWebExchange exchange,
                                      String correlationId,
                                      long startedAt) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int statusCode = status == null ? 200 : status.value();
        String message = "request completed correlationId={} method={} path={} status={} durationMs={}";
        Object[] arguments = {
                correlationId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                statusCode,
                elapsedMillis(startedAt)
        };
        if (statusCode >= 500) {
            log.error(message, arguments);
        } else if (statusCode >= 400) {
            log.warn(message, arguments);
        } else {
            log.info(message, arguments);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    @Override
    public int getOrder() {
        // Must run before authentication so rejected requests receive an id too.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
