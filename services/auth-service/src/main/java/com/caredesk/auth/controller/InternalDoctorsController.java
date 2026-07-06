package com.caredesk.auth.controller;

import com.caredesk.auth.service.UserService;
import org.openapitools.model.PaginatedUserProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service doctor directory, outside the public OpenAPI contract.
 *
 * <p>patient-service composes this into its {@code /doctors} listing because
 * auth-service owns doctor identity. The {@code /internal} prefix is not routed
 * by the API gateway, so this is reachable only pod-to-pod, guarded by the
 * NetworkPolicy and permitted anonymously in {@link com.caredesk.auth.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/internal/doctors")
public class InternalDoctorsController {

    private final UserService userService;

    public InternalDoctorsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PaginatedUserProfileResponse searchDoctors(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "specialization", required = false) String specialization,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return userService.searchDoctors(q, specialization, page, size);
    }
}
