package com.caredesk.patient.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an appointment id does not exist in the patient service
 * database. Mapped to HTTP 404 by Spring via {@link ResponseStatus}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AppointmentNotFoundException extends RuntimeException {

    /**
     * @param id the appointment id that was not found
     */
    public AppointmentNotFoundException(UUID id) {
        super("Appointment not found: " + id);
    }
}
