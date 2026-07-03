package com.tickets.discoveryservice.exception;

public class ApiException extends RuntimeException {
    // in production,
    public ApiException(String message) {
        super(message);
    }
}
