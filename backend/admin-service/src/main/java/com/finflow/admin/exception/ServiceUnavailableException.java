package com.finflow.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a required downstream service (application-service, notification-service)
 * is unreachable and the failure is non-recoverable for the current request.
 * Maps to HTTP 503 Service Unavailable.
 *
 * <p>Note: most downstream calls in AdminService swallow failures gracefully.
 * This exception is reserved for cases where continuing without the downstream
 * response would produce an incorrect or misleading result.</p>
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceUnavailableException extends RuntimeException {

    private final String serviceName;

    public ServiceUnavailableException(String serviceName, String reason) {
        super("Service '" + serviceName + "' is currently unavailable: " + reason);
        this.serviceName = serviceName;
    }

    public String getServiceName() { return serviceName; }
}