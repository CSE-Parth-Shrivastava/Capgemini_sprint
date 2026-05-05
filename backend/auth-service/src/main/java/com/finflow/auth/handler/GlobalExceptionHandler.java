package com.finflow.auth.handler;

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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.finflow.auth.exception.DuplicateResourceException;
import com.finflow.auth.exception.ErrorResponse;
import com.finflow.auth.exception.ForbiddenException;
import com.finflow.auth.exception.InvalidFileException;
import com.finflow.auth.exception.InvalidOperationException;
import com.finflow.auth.exception.ResourceNotFoundException;
import com.finflow.auth.exception.ErrorResponse.FieldError;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 Not Found ─────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────────
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateResourceException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // ── 422 Unprocessable Entity ───────────────────────────────────────────────
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Operation", ex.getMessage(), request);
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    // ── 403 Forbidden (Spring Security) ───────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action", request);
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    // ── 400 Bad Request — file validation ─────────────────────────────────────
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid File", ex.getMessage(), request);
    }

    // ── 400 Bad Request — file too large (Spring multipart limit) ─────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "File Too Large",
                "Upload exceeds the maximum allowed size", request);
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
                HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                "One or more fields failed validation", request.getRequestURI());
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── 400 Bad Request — malformed JSON body ─────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed or missing request body", request);
    }

    // ── 400 Bad Request — path/query param type mismatch ──────────────────────
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        String msg = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return build(HttpStatus.BAD_REQUEST, "Bad Request", msg, request);
    }

    // ── 400 Bad Request — missing required header ──────────────────────────────
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request",
                "Required header '" + ex.getHeaderName() + "' is missing", request);
    }

    // ── 400 Bad Request — general illegal argument ────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // ── 500 Internal Server Error — catch-all ─────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // ── helper ────────────────────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error,
                                                 String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), error, message, request.getRequestURI()));
    }
}