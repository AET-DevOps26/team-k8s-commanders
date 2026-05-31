package com.caredesk.patient.controller;

import org.openapitools.api.PatientsApi;
import org.springframework.stereotype.Controller;

// Scaffold: implements PatientsApi so Spring registers the routes, but does not
// override the default methods yet. The defaults return 501 Not Implemented.
// Actual endpoints (getPatientById, listPatientAppointments, etc.) come in #34.
@Controller
public class PatientsController implements PatientsApi {
}
