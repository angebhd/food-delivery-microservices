package com.amalitech.fooddelivery.orderservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback factory for the Delivery Service Feign client.
 * When the Delivery Service is DOWN, order retrieval must still succeed.
 * Delivery information is treated as non-critical enrichment data;
 * the fallback returns null so the caller gracefully degrades.
 */
@Slf4j
@Component
public class DeliveryInterfaceFallbackFactory implements FallbackFactory<DeliveryInterface> {

    @Override
    public DeliveryInterface create(Throwable cause) {
        return new DeliveryInterface() {

            @Override
            public DeliveryInfoResponse getByOrderId(Long orderId) {
                log.warn("Circuit breaker activated: Delivery Service is unavailable. " +
                        "Returning null delivery info for orderId={}. Cause: {}", orderId, cause.getMessage());
                return null;
            }
        };
    }
}

