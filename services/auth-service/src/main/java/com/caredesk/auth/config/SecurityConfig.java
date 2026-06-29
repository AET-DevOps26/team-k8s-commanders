package com.caredesk.auth.config;

import com.caredesk.auth.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Gateway owns the /api/v1 prefix — service only sees /auth/**
                        .requestMatchers("/auth/**").permitAll()
                        // User directory is readable by doctors; write/stats operations stay admin-only.
                        // Role comes from the JWT (set by JwtAuthFilter).
                        // More specific matchers first — Spring Security uses first match.
                        .requestMatchers(HttpMethod.GET, "/users/stats").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")
                        // Patient self-service — owner-or-admin enforced in UserAccountService.
                        .requestMatchers(HttpMethod.PUT, "/users/*/password").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/*").authenticated()
                        // GET /users/{id} is an internal service-to-service endpoint that
                        // patient-service and notes-service call without a JWT.
                        .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                        // Health and Prometheus endpoints must be reachable without auth.
                        // Both forms needed: Spring Security 6 `/**` doesn't match the exact path without trailing slash.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus").permitAll()
                        // Spring forwards unhandled exceptions to /error. Without permitting
                        // it, error pages come back as 403 instead of the intended status.
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
