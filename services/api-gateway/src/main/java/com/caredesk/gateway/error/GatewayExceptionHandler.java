package com.caredesk.gateway.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/** Converts failures raised before a downstream response into RFC 9457 responses. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements WebExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    private static final String UNEXPECTED_DETAIL = "An unexpected gateway error occurred";

    private final GatewayProblemDetails problemDetails;

    public GatewayExceptionHandler(GatewayProblemDetails problemDetails) {
        this.problemDetails = problemDetails;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        HttpStatusCode status;
        String detail;
        boolean upstreamFailure = false;
        if (hasCause(exception, TimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            detail = "Upstream service timed out";
            upstreamFailure = true;
        } else if (isUpstreamConnectionFailure(exception)) {
            status = HttpStatus.BAD_GATEWAY;
            detail = "Upstream service unavailable";
            upstreamFailure = true;
        } else if (exception instanceof ErrorResponseException errorResponse) {
            status = errorResponse.getStatusCode();
            detail = safeDetail(errorResponse, status);
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            detail = UNEXPECTED_DETAIL;
        }

        if (upstreamFailure) {
            LOGGER.warn("Upstream request failed with status {}: {}",
                    status.value(), exception.toString());
        } else if (status.is5xxServerError()) {
            LOGGER.error("Gateway request failed with status {}", status.value(), exception);
        }
        return problemDetails.write(exchange, status, detail);
    }

    private static String safeDetail(ErrorResponseException exception, HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return UNEXPECTED_DETAIL;
        }
        String detail = exception.getBody().getDetail();
        return detail == null || detail.isBlank() ? "Request failed" : detail;
    }

    private static boolean isUpstreamConnectionFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof WebClientRequestException
                    || current instanceof ConnectException
                    || current instanceof UnknownHostException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
