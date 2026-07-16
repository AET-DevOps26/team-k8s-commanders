package com.caredesk.patient.controller;

import com.caredesk.common.web.ApiExceptionHandler;
import com.caredesk.patient.service.AppointmentNotFoundException;
import com.caredesk.patient.service.AppointmentStateConflictException;
import com.caredesk.patient.service.DoctorNotFoundException;
import com.caredesk.patient.service.SlotNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Patient/appointment exception mapping. Per-record ownership denials
 * (issue #172) render as RFC 9457 403s via the inherited handler.
 */
@RestControllerAdvice
public class PatientExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler({AppointmentNotFoundException.class, DoctorNotFoundException.class,
            SlotNotFoundException.class})
    ProblemDetail notFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AppointmentStateConflictException.class)
    ProblemDetail conflict(AppointmentStateConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }
}
