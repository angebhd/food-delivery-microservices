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

    // MONOLITH: cross-domain data embedded directly
    private Long customerId;
    private String customerName;
    private Long restaurantId;
    private String restaurantName;

    // Delivery info embedded (monolith convenience)
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

        // MONOLITH: cross-domain entity traversal
        dto.setCustomerId(o.getCustomerId());
//        dto.setCustomerName(o.getCustomer().getFirstName() + " " + o.getCustomer().getLastName()); // TODO
        dto.setRestaurantId(o.getRestaurantId());
//        dto.setRestaurantName(o.getRestaurant().getName()); // TODO

        // MONOLITH: cross-domain delivery info
        // TODO
//        if (o.getDeliveryId() != null) {
//            dto.setDeliveryStatus(o.getDelivery().getStatus().name());
//            dto.setDriverName(o.getDelivery().getDriverName());
//            dto.setDriverPhone(o.getDelivery().getDriverPhone());
//        }

        // Map order items with menu item names from Restaurant domain
        dto.setItems(o.getItems().stream().map(item -> {
            OrderItemDetail detail = new OrderItemDetail();
            detail.setId(item.getId());
//            detail.setItemName(item.getMenuItem().getName()); // MONOLITH: cross-domain! // TODO
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getUnitPrice());
            detail.setSubtotal(item.getSubtotal());
            return detail;
        }).toList());

        return dto;
    }
}
