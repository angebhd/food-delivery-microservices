package com.amalitech.fooddelivery.orderservice.client;

import com.amalitech.fooddelivery.orderservice.dto.MenuItemResponse;
import com.amalitech.fooddelivery.orderservice.dto.RestaurantResponse;
import com.amalitech.fooddelivery.orderservice.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory for the Restaurant Service Feign client.
 * When the Restaurant Service is DOWN, order placement must be rejected
 * with a clear business error (not a timeout or raw stacktrace).
 */
@Slf4j
@Component
public class RestaurantInterfaceFallbackFactory implements FallbackFactory<RestaurantInterface> {

    @Override
    public RestaurantInterface create(Throwable cause) {
        return new RestaurantInterface() {

            @Override
            public RestaurantResponse findEntityById(Long id) {
                log.error("Circuit breaker activated: Restaurant Service is unavailable. " +
                        "Cannot validate restaurant id={}. Cause: {}", id, cause.getMessage());
                throw new ServiceUnavailableException("Restaurant Service", cause);
            }

            @Override
            public MenuItemResponse getMenuItemById(Long id) {
                log.error("Circuit breaker activated: Restaurant Service is unavailable. " +
                        "Cannot validate menu item id={}. Cause: {}", id, cause.getMessage());
                throw new ServiceUnavailableException("Restaurant Service", cause);
            }
        };
    }
}

