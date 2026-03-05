package com.amalitech.fooddelivery.restaurantservice.client;
import com.amalitech.fooddelivery.restaurantservice.dto.CustomerResponse;
import com.amalitech.fooddelivery.restaurantservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
/**
 * Fallback factory for the Customer Service Feign client used by Restaurant Service.
 * When the Customer Service is DOWN, restaurant ownership validation and
 * owner name enrichment degrade gracefully.
 */
@Slf4j
@Component
public class CustomerInterfaceFallbackFactory implements FallbackFactory<CustomerInterface> {
    @Override
    public CustomerInterface create(Throwable cause) {
        return new CustomerInterface() {
            @Override
            public CustomerResponse findEntityByUsername(String username) {
                log.error("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot validate owner username={}. Cause: {}", username, cause.getMessage());
                throw new ServiceUnavailableException("Customer Service", cause);
            }
            @Override
            public CustomerResponse getById(Long id) {
                log.warn("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot fetch owner info for id={}. Cause: {}", id, cause.getMessage());
                return null;
            }
            @Override
            public CustomerResponse makeRestaurantOwner() {
                log.error("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot promote user to restaurant owner. Cause: {}", cause.getMessage());
                throw new ServiceUnavailableException("Customer Service", cause);
            }
        };
    }
}
