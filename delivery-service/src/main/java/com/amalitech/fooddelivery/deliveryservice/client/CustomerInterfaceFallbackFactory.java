package com.amalitech.fooddelivery.deliveryservice.client;
import com.amalitech.fooddelivery.deliveryservice.dto.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
/**
 * Fallback factory for the Customer Service Feign client used by Delivery Service.
 * When the Customer Service is DOWN, customer info enrichment degrades gracefully.
 */
@Slf4j
@Component
public class CustomerInterfaceFallbackFactory implements FallbackFactory<CustomerInterface> {
    @Override
    public CustomerInterface create(Throwable cause) {
        return new CustomerInterface() {
            @Override
            public CustomerResponse findEntityByUsername(String username) {
                log.warn("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot fetch customer by username={}. Cause: {}", username, cause.getMessage());
                return null;
            }
            @Override
            public CustomerResponse getById(Long id) {
                log.warn("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot fetch customer by id={}. Cause: {}", id, cause.getMessage());
                return null;
            }
        };
    }
}
