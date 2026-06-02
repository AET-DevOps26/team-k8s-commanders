package com.caredesk.patient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the patient service.
 *
 * <p>The service sits behind the API gateway and trusts the
 * {@code X-User-Email} and {@code X-User-Role} headers set by the gateway
 * once it has validated the JWT. The patient service therefore does not
 * re-validate the JWT itself. {@link PatientHeaderAuthFilter} translates
 * those headers into an authenticated Spring Security principal.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PatientHeaderAuthFilter headerAuthFilter;

    /**
     * Creates the security configuration.
     *
     * @param headerAuthFilter filter that promotes trusted gateway headers
     *                         into the Spring Security context
     */
    public SecurityConfig(PatientHeaderAuthFilter headerAuthFilter) {
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
                        // Health endpoint must be reachable for the Docker healthcheck.
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Spring forwards unhandled exceptions to /error. Without permitting
                        // it, error pages come back as 403 instead of the intended status.
                        .requestMatchers("/error").permitAll()
                        // Everything else requires an authenticated principal, which
                        // PatientHeaderAuthFilter sets from the gateway-injected headers.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
