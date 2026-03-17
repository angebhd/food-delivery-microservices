package com.amalitech.fooddelivery.apigateway.exception;

/**
 * Thrown when an upstream service is unavailable and the circuit breaker fallback is triggered.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is currently unavailable. Please try again later.", cause);
    }
}
