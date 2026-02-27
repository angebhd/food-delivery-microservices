package com.amalitech.fooddelivery.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event received from the Delivery Service via RabbitMQ when a delivery status changes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryUpdateEvent {
    private Long orderId;
    private String status;
}

