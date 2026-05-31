package com.caredesk.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages keeps Spring from picking up the generated org.openapitools
// controllers (PatientsApiController etc.) from the stubs JAR. Our own controllers
// in com.caredesk.patient.controller implement the API interfaces directly.
@SpringBootApplication(scanBasePackages = "com.caredesk.patient")
public class PatientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientServiceApplication.class, args);
    }
}
