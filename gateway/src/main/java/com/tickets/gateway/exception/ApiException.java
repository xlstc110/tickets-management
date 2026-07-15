package com.tickets.gateway.exception;

public class ApiException extends RuntimeException {
    // in production,
    public ApiException(String message) {
        super(message);
    }
}
