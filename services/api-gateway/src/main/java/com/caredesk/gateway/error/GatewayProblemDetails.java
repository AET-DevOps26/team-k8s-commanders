package com.caredesk.gateway.error;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

/** Writes RFC 9457 error responses produced at the gateway boundary. */
@Component
public class GatewayProblemDetails {

    private final ObjectMapper objectMapper;

    public GatewayProblemDetails(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatusCode status,
            String detail) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(new IllegalStateException("Response is already committed"));
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(exchange.getRequest().getURI().getRawPath()));
        byte[] body = objectMapper.writeValueAsBytes(problem);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body)));
    }
}
