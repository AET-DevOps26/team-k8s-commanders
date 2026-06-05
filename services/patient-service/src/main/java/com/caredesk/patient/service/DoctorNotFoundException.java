package com.caredesk.patient.service;

import java.util.UUID;

public class DoctorNotFoundException extends RuntimeException {

    public DoctorNotFoundException(UUID doctorId) {
        super("Doctor not found: " + doctorId);
    }
}
