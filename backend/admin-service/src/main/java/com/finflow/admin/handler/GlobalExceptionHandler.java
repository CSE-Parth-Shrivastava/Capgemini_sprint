package com.finflow.admin.handler;

import com.finflow.admin.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for the admin-service.
 *
 * <p>Every exception that can be thrown by the admin workflow is mapped here to a
 * structured {@link ErrorResponse} with an appropriate HTTP status. The handler
 * ensures no raw stack traces ever reach the client.</p>
 *
 * <p>Handler precedence (most specific first):</p>
 * <ol>
 *   <li>404  — {@link ResourceNotFoundException}</li>
 *   <li>409  — {@link DuplicateDecisionException}</li>
 *   <li>422  — {@link InvalidOperationException}</li>
 *   <li>400  — {@link InvalidDecisionException}</li>
 *   <li>503  — {@link ServiceUnavailableException}</li>
 *   <li>403  — {@link AccessDeniedException} (Spring Security)</li>
 *   <li>401  — {@link BadCredentialsException}</li>
 *   <li>400  — {@link MethodArgumentNotValidException} (bean validation)</li>
 *   <li>400  — {@link HttpMessageNotReadableException} (malformed JSON)</li>
 *   <li>400  — {@link MethodArgumentTypeMismatchException} (path/query param)</li>
 *   <li>400  — {@link MissingRequestHeaderException}</li>
 *   <li>400  — {@link IllegalArgumentException} (e.g. bad enum value)</li>
 *   <li>500  — {@link Exception} (catch-all)</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateDecisionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDecision(DuplicateDecisionException ex,
                                                                   HttpServletRequest request) {
        log.warn("Duplicate decision attempt at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // ── 422 Unprocessable Entity ───────────────────────────────────────────────

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex,
                                                                  HttpServletRequest request) {
        log.warn("Invalid operation at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Operation", ex.getMessage(), request);
    }

    // ── 400 Bad Request — invalid decision value ──────────────────────────────

    @ExceptionHandler(InvalidDecisionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDecision(InvalidDecisionException ex,
                                                                HttpServletRequest request) {
        log.warn("Invalid decision value '{}' at {}", ex.getProvidedValue(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // ── 503 Service Unavailable ───────────────────────────────────────────────

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex,
                                                                    HttpServletRequest request) {
        log.error("Downstream service '{}' unavailable at {}: {}",
                ex.getServiceName(), request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage(), request);
    }

    // ── 403 Forbidden — Spring Security AccessDeniedException ─────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                              HttpServletRequest request) {
        // Do NOT log at ERROR — 403s from non-admin callers are expected traffic
        log.warn("Access denied at {} for principal: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action.", request);
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    // ── 400 Bad Request — @Valid bean validation failures ─────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        BindingResult br = ex.getBindingResult();
        List<ErrorResponse.FieldError> fieldErrors = br.getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more fields failed validation.",
                request.getRequestURI());
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 400 Bad Request — malformed or missing JSON body ─────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        log.warn("Malformed request body at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Malformed or missing request body.", request);
    }

    // ── 400 Bad Request — path/query param type mismatch ──────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        String msg = "Invalid value '" + ex.getValue() +
                     "' for parameter '" + ex.getName() + "'.";
        return build(HttpStatus.BAD_REQUEST, "Bad Request", msg, request);
    }

    // ── 400 Bad Request — missing required header ──────────────────────────────

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                              HttpServletRequest request) {
        String msg = "Required header '" + ex.getHeaderName() + "' is missing.";
        return build(HttpStatus.BAD_REQUEST, "Bad Request", msg, request);
    }

    // ── 400 Bad Request — IllegalArgumentException ────────────────────────────
    // Catches raw DecisionType.valueOf() failures if they ever escape the service layer.

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // ── 500 Internal Server Error — catch-all ─────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error,
                                                 String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), error, message, request.getRequestURI()));
    }
}