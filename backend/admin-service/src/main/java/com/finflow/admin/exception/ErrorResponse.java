package com.finflow.admin.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Uniform error envelope returned by {@link com.finflow.admin.handler.GlobalExceptionHandler}
 * for every non-2xx response from the admin-service.
 *
 * <pre>
 * {
 *   "timestamp" : "2026-05-04T10:15:30",
 *   "status"    : 404,
 *   "error"     : "Not Found",
 *   "message"   : "Decision not found with id: 42",
 *   "path"      : "/admin/decisions/42",
 *   "fieldErrors": [...]   // only present for validation failures
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int    status;
    private final String error;
    private final String message;
    private final String path;

    /** Only populated for 400 validation errors. */
    private List<FieldError> fieldErrors;

    public ErrorResponse(int status, String error, String message, String path) {
        this.status  = status;
        this.error   = error;
        this.message = message;
        this.path    = path;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public LocalDateTime getTimestamp()          { return timestamp; }
    public int           getStatus()             { return status; }
    public String        getError()              { return error; }
    public String        getMessage()            { return message; }
    public String        getPath()               { return path; }
    public List<FieldError> getFieldErrors()     { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fe) { this.fieldErrors = fe; }

    // ── Nested: per-field validation error ───────────────────────────────────

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field   = field;
            this.message = message;
        }

        public String getField()   { return field; }
        public String getMessage() { return message; }
    }
}