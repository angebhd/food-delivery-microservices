package com.amalitech.fooddelivery.deliveryservice.dto;

import com.amalitech.fooddelivery.deliveryservice.entity.DeliveryEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeliveryResponse {
    private Long id;
    private String status;
    private String driverName;
    private String driverPhone;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;

    // Cross-domain data enriched via Feign calls in the service layer
    private Long orderId;
    private String orderStatus;
    private Long customerId;
    private String customerName;
    private String restaurantName;

    public static DeliveryResponse fromEntity(DeliveryEntity d) {
        DeliveryResponse dto = new DeliveryResponse();
        dto.setId(d.getId());
        dto.setStatus(d.getStatus().name());
        dto.setDriverName(d.getDriverName());
        dto.setDriverPhone(d.getDriverPhone());
        dto.setPickupAddress(d.getPickupAddress());
        dto.setDeliveryAddress(d.getDeliveryAddress());
        dto.setAssignedAt(d.getAssignedAt());
        dto.setPickedUpAt(d.getPickedUpAt());
        dto.setDeliveredAt(d.getDeliveredAt());
        dto.setCreatedAt(d.getCreatedAt());

        // Cross-domain ID stored locally; remaining fields enriched in the service layer
        dto.setOrderId(d.getOrderId());
        return dto;
    }
}
