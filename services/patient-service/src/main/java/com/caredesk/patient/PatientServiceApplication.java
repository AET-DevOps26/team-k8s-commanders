package com.caredesk.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CareDesk patient and appointment service.
 *
 * <p>{@code scanBasePackages} keeps Spring from picking up the generated
 * {@code org.openapitools} controllers (such as {@code PatientsApiController})
 * from the OpenAPI stubs JAR. Our own controllers in
 * {@code com.caredesk.patient.controller} implement the API interfaces
 * directly.
 */
@SpringBootApplication(scanBasePackages = "com.caredesk.patient")
public class PatientServiceApplication {

    /**
     * Boots the Spring application context.
     *
     * @param args command-line arguments forwarded to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}
