package com.finflow.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business rule is violated — e.g. a decision has already been
 * made on an application that is being decided on again.
 * Maps to HTTP 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}