package com.caredesk.patient.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an operation is rejected because the appointment is not in a
 * compatible state, for example rescheduling an already-cancelled
 * appointment. Mapped to HTTP 409 by Spring via {@link ResponseStatus}.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AppointmentStateConflictException extends RuntimeException {

    /**
     * @param message a human-readable reason why the operation is rejected
     */
    public AppointmentStateConflictException(String message) {
        super(message);
    }
}
