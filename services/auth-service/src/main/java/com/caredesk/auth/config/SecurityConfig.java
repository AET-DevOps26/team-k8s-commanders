package com.caredesk.auth.config;

import com.caredesk.auth.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                exceptionResolver.resolveException(request, response, null,
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Authentication is required")))
                        .accessDeniedHandler((request, response, exception) ->
                                exceptionResolver.resolveException(request, response, null,
                                        new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Access is denied"))))
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
                        // /internal/** is not routed by the gateway; reachable only pod-to-pod
                        // for cross-service composition (e.g. patient-service doctor directory).
                        .requestMatchers(HttpMethod.GET, "/internal/**").permitAll()
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
        // Account-enumeration hardening (issue tracked as "login responses enable
        // account enumeration"):
        //
        // 1. Keep Spring's default hideUserNotFoundExceptions=true, so an unknown
        //    email surfaces as BadCredentialsException — indistinguishable from a
        //    wrong password — and the provider runs a dummy BCrypt hash for
        //    unknown users, closing the timing side-channel.
        //
        // 2. Move the account-status (disabled) check from before password
        //    verification to after it. By default a deactivated account is
        //    reported without checking the password, which leaks account
        //    existence to anyone probing emails. With the check moved, only a
        //    caller who knows the correct password learns the account is
        //    deactivated; everyone else gets the generic failure.
        provider.setPreAuthenticationChecks(user -> {
            // deliberately empty — status checks run post-authentication
        });
        provider.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
