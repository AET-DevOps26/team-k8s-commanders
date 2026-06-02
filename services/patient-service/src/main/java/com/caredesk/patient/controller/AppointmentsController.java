package com.caredesk.patient.controller;

import org.openapitools.api.AppointmentsApi;
import org.springframework.stereotype.Controller;

/**
 * Scaffold controller for the {@code /appointments/**} endpoints.
 *
 * <p>Implements {@link AppointmentsApi} so Spring registers the routes. The
 * default 501 behaviour holds until the real handlers land in issue #36.
 */
@Controller
public class AppointmentsController implements AppointmentsApi {
}
