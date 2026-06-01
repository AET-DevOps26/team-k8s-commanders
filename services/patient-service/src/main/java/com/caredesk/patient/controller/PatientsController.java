package com.caredesk.patient.controller;

import org.openapitools.api.PatientsApi;
import org.springframework.stereotype.Controller;

/**
 * Scaffold controller for the {@code /patients/**} endpoints.
 *
 * <p>Implements {@link PatientsApi} so Spring registers the routes, but does
 * not override any of the default methods yet. Every endpoint therefore
 * returns 501 Not Implemented until the real handlers
 * ({@code getPatientById}, {@code listPatientAppointments} and friends) land
 * in issue #34.
 */
@Controller
public class PatientsController implements PatientsApi {
}
