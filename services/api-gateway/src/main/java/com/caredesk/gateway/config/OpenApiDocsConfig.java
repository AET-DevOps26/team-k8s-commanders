package com.caredesk.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.resource.LiteWebJarsResourceResolver;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

// Serves the bundled OpenAPI spec and a self-hosted Swagger UI from the gateway:
//   GET /api/v1/openapi.yaml  -> the spec (static resource)
//   GET /api/v1/docs          -> Swagger UI (index.html below)
//   GET /api/v1/webjars/**    -> swagger-ui assets (resolved from the webjar)
// Everything lives under /api/v1 because that is the only prefix every front
// proxy forwards to the gateway. These paths match no route, so they are served
// directly and never hit the JWT filter (i.e. public, no PUBLIC_PATHS entry).
@Configuration
public class OpenApiDocsConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // The lite resolver injects the webjar version, keeping it pinned only in the pom.
        registry.addResourceHandler("/api/v1/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true)
                .addResolver(new LiteWebJarsResourceResolver());
    }

    @Bean
    public RouterFunction<ServerResponse> swaggerUiRoute() {
        Resource index = new ClassPathResource("static/api/v1/docs/index.html");
        return route(GET("/api/v1/docs"),
                request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).bodyValue(index));
    }
}
