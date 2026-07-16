package com.caredesk.auth.controller;

import com.caredesk.auth.exception.LoginFailedException;
import com.caredesk.auth.exception.ValidationException;
import com.caredesk.auth.service.DuplicateEmailException;
import com.caredesk.auth.service.UserNotFoundException;
import com.caredesk.common.web.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler extends ApiExceptionHandler {

    @ExceptionHandler(LoginFailedException.class)
    ProblemDetail loginFailed(LoginFailedException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    ProblemDetail validation(ValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ProblemDetail duplicateEmail(DuplicateEmailException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail notFound(UserNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail unauthorized(BadCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }
}
