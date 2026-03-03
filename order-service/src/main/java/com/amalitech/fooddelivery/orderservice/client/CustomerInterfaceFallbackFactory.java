package com.amalitech.fooddelivery.orderservice.client;

import com.amalitech.fooddelivery.orderservice.dto.CustomerResponse;
import com.amalitech.fooddelivery.orderservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory for the Customer Service Feign client.
 * When the Customer Service is DOWN, order operations that require
 * customer validation must fail with a clear business error.
 */
@Slf4j
@Component
public class CustomerInterfaceFallbackFactory implements FallbackFactory<CustomerInterface> {

    @Override
    public CustomerInterface create(Throwable cause) {
        return new CustomerInterface() {

            @Override
            public CustomerResponse findEntityByUsername(String username) {
                log.error("Circuit breaker activated: Customer Service is unavailable. " +
                        "Cannot validate customer username={}. Cause: {}", username, cause.getMessage());
                throw new ServiceUnavailableException("Customer Service", cause);
            }
        };
    }
}

