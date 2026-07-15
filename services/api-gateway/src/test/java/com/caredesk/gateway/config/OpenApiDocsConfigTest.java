package com.caredesk.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class OpenApiDocsConfigTest {

    @Test
    void swaggerUiRouteServesBundledHtml() {
        WebTestClient client = WebTestClient
                .bindToRouterFunction(new OpenApiDocsConfig().swaggerUiRoute())
                .build();

        client.get()
                .uri("/api/v1/docs")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body)
                        .contains("SwaggerUIBundle"));
    }
}
