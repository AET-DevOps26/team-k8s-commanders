package com.caredesk.patient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Patient service sits behind the API gateway and trusts the X-User-Email and
// X-User-Role headers set by the gateway after it validates the JWT.
// We do not re-validate the JWT here. Controllers read the trusted headers
// directly to authorise requests (see PatientHeaderAuthFilter).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PatientHeaderAuthFilter headerAuthFilter;

    public SecurityConfig(PatientHeaderAuthFilter headerAuthFilter) {
        this.headerAuthFilter = headerAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Health endpoint must be reachable for the Docker healthcheck.
                        .requestMatchers("/actuator/health/**").permitAll()
                        // Everything else requires an authenticated principal, which
                        // PatientHeaderAuthFilter sets from the gateway-injected headers.
                        .anyRequest().authenticated()
                )
                .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
