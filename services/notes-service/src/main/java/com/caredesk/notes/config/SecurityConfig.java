package com.caredesk.notes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the notes service.
 *
 * <p>The service sits behind the API gateway and trusts the
 * {@code X-User-Email} and {@code X-User-Role} headers set by the gateway
 * once it has validated the JWT. The notes service therefore does not
 * re-validate the JWT itself. {@link NotesHeaderAuthFilter} translates
 * those headers into an authenticated Spring Security principal.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final NotesHeaderAuthFilter headerAuthFilter;

    /**
     * Creates the security configuration.
     *
     * @param headerAuthFilter filter that promotes trusted gateway headers
     *                         into the Spring Security context
     */
    public SecurityConfig(NotesHeaderAuthFilter headerAuthFilter) {
        this.headerAuthFilter = headerAuthFilter;
    }

    /**
     * Builds the security filter chain.
     *
     * <p>CSRF is disabled because the service is stateless and never serves
     * browser forms. Only {@code /actuator/health/**} is permitted
     * anonymously, so the Docker healthcheck can reach it. Every other
     * request requires the trusted headers to be present.
     *
     * @param http the {@link HttpSecurity} builder injected by Spring
     * @return the configured filter chain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // Spring forwards errors internally to /error — must be reachable
                        // without authentication or the real status code is swallowed by 401.
                        .requestMatchers("/error").permitAll()
                        // Note endpoints are restricted to doctors. Reads are shared
                        // across doctors (clinical-record continuity); authoring is
                        // enforced per-request for writes and deletes in the service.
                        .requestMatchers(HttpMethod.GET, "/appointments/*/note").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/appointments/*/note").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/appointments/*/note").hasRole("DOCTOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
