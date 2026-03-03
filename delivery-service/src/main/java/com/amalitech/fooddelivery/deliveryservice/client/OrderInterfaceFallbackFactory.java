package com.amalitech.fooddelivery.deliveryservice.client;
import com.amalitech.fooddelivery.deliveryservice.dto.OrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
/**
 * Fallback factory for the Order Service Feign client used by Delivery Service.
 * When the Order Service is DOWN, delivery enrichment with order info degrades gracefully
 * by returning null, allowing the core delivery data to still be returned.
 */
@Slf4j
@Component
public class OrderInterfaceFallbackFactory implements FallbackFactory<OrderInterface> {
    @Override
    public OrderInterface create(Throwable cause) {
        return new OrderInterface() {
            @Override
            public OrderResponse getById(Long id) {
                log.warn("Circuit breaker activated: Order Service is unavailable. "
                        + "Cannot fetch order info for id={}. Cause: {}", id, cause.getMessage());
                return null;
            }
        };
    }
}
