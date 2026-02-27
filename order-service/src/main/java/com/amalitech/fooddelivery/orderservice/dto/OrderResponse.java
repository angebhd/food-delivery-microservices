package com.amalitech.fooddelivery.orderservice.dto;

import com.amalitech.fooddelivery.orderservice.entity.OrderEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private String deliveryAddress;
    private String specialInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDeliveryTime;
    private List<OrderItemDetail> items;

    // Cross-domain data from snapshot fields stored at order placement time
    private Long customerId;
    private String customerName;
    private Long restaurantId;
    private String restaurantName;
    private String restaurantAddress;

    // Delivery info enriched via Feign call at read time
    private String deliveryStatus;
    private String driverName;
    private String driverPhone;

    @Data
    public static class OrderItemDetail {
        private Long id;
        private String itemName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    public static OrderResponse fromEntity(OrderEntity o) {
        OrderResponse dto = new OrderResponse();
        dto.setId(o.getId());
        dto.setStatus(o.getStatus().name());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setDeliveryFee(o.getDeliveryFee());
        dto.setDeliveryAddress(o.getDeliveryAddress());
        dto.setSpecialInstructions(o.getSpecialInstructions());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setEstimatedDeliveryTime(o.getEstimatedDeliveryTime());

        // Cross-domain data from snapshot fields stored at order placement time
        dto.setCustomerId(o.getCustomerId());
        dto.setCustomerName(o.getCustomerName());
        dto.setRestaurantId(o.getRestaurantId());
        dto.setRestaurantName(o.getRestaurantName());
        dto.setRestaurantAddress(o.getRestaurantAddress());

        // Map order items with snapshot item names
        dto.setItems(o.getItems().stream().map(item -> {
            OrderItemDetail detail = new OrderItemDetail();
            detail.setId(item.getId());
            detail.setItemName(item.getItemName());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getSubtotal());
            return detail;
        }).toList());

        return dto;
    }
}
