package com.caredesk.notes.controller;

import com.caredesk.common.web.ApiExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Central RFC 9457 exception mapping for notes endpoints. */
@RestControllerAdvice
public class NotesExceptionHandler extends ApiExceptionHandler {
}
