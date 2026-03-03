package com.amalitech.fooddelivery.orderservice.exception;

/**
 * Thrown when an upstream service (e.g., Restaurant Service, Customer Service)
 * is unavailable and the circuit breaker fallback is triggered.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is currently unavailable. Please try again later.", cause);
    }
}

