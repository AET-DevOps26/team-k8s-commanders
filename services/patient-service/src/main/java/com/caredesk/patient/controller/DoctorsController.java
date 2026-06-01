package com.caredesk.patient.controller;

import org.openapitools.api.DoctorsApi;
import org.springframework.stereotype.Controller;

/**
 * Scaffold controller for the {@code /doctors/**} endpoints.
 *
 * <p>Implements {@link DoctorsApi} so Spring registers the routes. The
 * default 501 behaviour holds until the real handlers land in issue #35.
 */
@Controller
public class DoctorsController implements DoctorsApi {
}
