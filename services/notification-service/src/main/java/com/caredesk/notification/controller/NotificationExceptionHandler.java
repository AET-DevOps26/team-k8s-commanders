package com.caredesk.notification.controller;

import com.caredesk.common.web.ApiExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Central RFC 9457 exception mapping for notification endpoints. */
@RestControllerAdvice
public class NotificationExceptionHandler extends ApiExceptionHandler {
}
