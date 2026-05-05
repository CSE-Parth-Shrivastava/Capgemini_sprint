package com.finflow.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an admin attempts to record a second decision on an application
 * that has already been decided (APPROVED or REJECTED).
 * Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateDecisionException extends RuntimeException {

    private final Long applicationId;

    public DuplicateDecisionException(Long applicationId) {
        super("A decision has already been recorded for application id: " + applicationId);
        this.applicationId = applicationId;
    }

    public Long getApplicationId() { return applicationId; }
}