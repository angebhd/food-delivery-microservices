package com.amalitech.fooddelivery.orderservice.client;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Lightweight DTO used to receive delivery information from the Delivery Service
 * via Feign. Only the fields needed by the Order Service are included.
 */
@Data
public class DeliveryInfoResponse {
    private Long id;
    private String status;
    private String driverName;
    private String driverPhone;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
}

