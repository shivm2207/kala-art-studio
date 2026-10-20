
package com.kala.backend.error;

import org.springframework.http.HttpStatus;

// Carries the HTTP status we want to return for an expected business-rule failure.
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
