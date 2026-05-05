package com.finflow.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (Decision, Application) cannot be found.
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(resourceName + " not found with id: " + resourceId);
        this.resourceName = resourceName;
        this.resourceId   = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = null;
        this.resourceId   = null;
    }

    public String getResourceName() { return resourceName; }
    public Object getResourceId()   { return resourceId; }
}