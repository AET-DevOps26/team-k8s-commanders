package com.caredesk.notes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CareDesk clinical notes service.
 *
 * <p>{@code scanBasePackages} keeps Spring from picking up the generated
 * {@code org.openapitools} controllers (such as {@code AppointmentsApiController})
 * from the OpenAPI stubs JAR. Our own controllers in
 * {@code com.caredesk.notes.controller} implement the API interfaces
 * directly.
 */
@SpringBootApplication(scanBasePackages = "com.caredesk.notes")
public class NotesServiceApplication {

    /**
     * Boots the Spring application context.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(NotesServiceApplication.class, args);
    }
}
