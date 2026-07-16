package com.caredesk.gateway.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler handler = new GatewayExceptionHandler(
            new GatewayProblemDetails(new ObjectMapper()));

    @Test
    void preservesSafeClientErrorStatusAndDetail() {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"status\":404")
                .contains("\"detail\":\"Route not found\"")
                .contains("\"instance\":\"/missing\"");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new IllegalStateException("database password leaked"))
                .block();

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body)
                .contains("\"detail\":\"An unexpected gateway error occurred\"")
                .doesNotContain("database password leaked");
    }

    @Test
    void mapsNestedConnectionFailuresToBadGateway() {
        MockServerWebExchange exchange = exchange();

        handler.handle(exchange, new IllegalStateException(
                "routing failed",
                new ConnectException("Connection refused"))).block();

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(body)
                .contains("\"detail\":\"Upstream service unavailable\"")
                .doesNotContain("Connection refused");
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/missing").build());
    }
}
