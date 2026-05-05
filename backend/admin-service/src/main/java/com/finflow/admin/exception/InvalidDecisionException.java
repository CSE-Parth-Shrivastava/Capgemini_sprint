package com.finflow.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the decision field in a {@link com.finflow.admin.dto.DecisionRequest}
 * contains an unrecognised value (i.e. not APPROVED or REJECTED).
 * Maps to HTTP 400 Bad Request.
 *
 * <p>This gives callers a clear, structured error instead of the raw
 * {@link IllegalArgumentException} that {@code DecisionType.valueOf()} would throw.</p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDecisionException extends RuntimeException {

    private final String providedValue;

    public InvalidDecisionException(String providedValue) {
        super("Invalid decision value '" + providedValue +
              "'. Accepted values are: APPROVED, REJECTED");
        this.providedValue = providedValue;
    }

    public String getProvidedValue() { return providedValue; }
}