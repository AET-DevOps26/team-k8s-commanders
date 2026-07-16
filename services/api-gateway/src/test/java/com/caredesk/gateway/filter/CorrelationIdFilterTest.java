package com.caredesk.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesAndForwardsValidCorrelationId() {
        MockServerWebExchange exchange = exchange("request-123");
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, captured -> {
            forwarded.set(captured);
            captured.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("request-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("request-123");
    }

    @Test
    void replacesUnsafeCorrelationIdAndLogsClientError() {
        MockServerWebExchange exchange = exchange("unsafe id");
        AtomicReference<String> forwardedId = new AtomicReference<>();

        filter.filter(exchange, forwarded -> {
            forwardedId.set(forwarded.getRequest().getHeaders()
                    .getFirst(CorrelationIdFilter.HEADER_NAME));
            forwarded.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return Mono.empty();
        }).block();

        assertThat(UUID.fromString(forwardedId.get())).isNotNull();
    }

    @Test
    void logsServerErrorStatus() {
        MockServerWebExchange exchange = exchange(null);
        filter.filter(exchange, forwarded -> {
            forwarded.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
            return Mono.empty();
        }).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void logsAndPropagatesUnhandledFailure() {
        MockServerWebExchange exchange = exchange("request-456");
        Mono<Void> result = filter.filter(exchange,
                ignored -> Mono.error(new IllegalStateException("boom")));

        assertThatThrownBy(result::block).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runsBeforeAuthenticationFilter() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    private static MockServerWebExchange exchange(String correlationId) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/api/v1/patients");
        if (correlationId != null) {
            builder.header(CorrelationIdFilter.HEADER_NAME, correlationId);
        }
        return MockServerWebExchange.from(builder.build());
    }
}
