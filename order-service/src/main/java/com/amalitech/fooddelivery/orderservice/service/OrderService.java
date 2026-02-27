package com.amalitech.fooddelivery.orderservice.service;

import com.amalitech.fooddelivery.orderservice.client.DeliveryInfoResponse;
import com.amalitech.fooddelivery.orderservice.client.CustomerInterface;
import com.amalitech.fooddelivery.orderservice.client.DeliveryInterface;
import com.amalitech.fooddelivery.orderservice.client.RestaurantInterface;
import com.amalitech.fooddelivery.orderservice.config.RabbitMQConfig;
import com.amalitech.fooddelivery.orderservice.dto.*;
import com.amalitech.fooddelivery.orderservice.entity.OrderEntity;
import com.amalitech.fooddelivery.orderservice.entity.OrderItemEntity;
import com.amalitech.fooddelivery.orderservice.exception.ResourceNotFoundException;
import com.amalitech.fooddelivery.orderservice.exception.UnauthorizedException;
import com.amalitech.fooddelivery.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Service business logic.
 *
 * Cross-domain communication:
 *  - Validates customer via Feign call to Customer Service
 *  - Validates restaurant and menu items via Feign call to Restaurant Service
 *  - Publishes OrderPlacedEvent to RabbitMQ; Delivery Service subscribes asynchronously
 *  - Enriches order responses with delivery info via Feign call to Delivery Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerInterface customerService;
    private final RestaurantInterface restaurantService;
    private final DeliveryInterface deliveryService;
    private final RabbitTemplate rabbitTemplate;


    @Transactional
    public OrderResponse placeOrder(String customerUsername, PlaceOrderRequest request) {
        CustomerResponse customer = customerService.findEntityByUsername(customerUsername);

        RestaurantResponse restaurant = restaurantService.findEntityById(request.getRestaurantId());

        if (!restaurant.isActive()) {
            throw new IllegalStateException("Restaurant is currently not accepting orders");
        }

        // Build order
        OrderEntity order = OrderEntity.builder()
                .customerId(customer.getId())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .restaurantAddress(restaurant.getAddress())
                .deliveryAddress(request.getDeliveryAddress() != null
                        ? request.getDeliveryAddress()
                        : customer.getDeliveryAddress())
                .specialInstructions(request.getSpecialInstructions())
                .estimatedDeliveryTime(
                        LocalDateTime.now().plusMinutes(restaurant.getEstimatedDeliveryMinutes()))
                .build();

        // Validate and price each menu item via Restaurant Service
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItemResponse menuItem = restaurantService.getMenuItemById(itemReq.getMenuItemId());

            if (!menuItem.isAvailable()) {
                throw new IllegalStateException("Menu item '" + menuItem.getName() + "' is not available");
            }
            if (!menuItem.getRestaurantId().equals(restaurant.getId())) {
                throw new IllegalStateException("Menu item '" + menuItem.getName()
                        + "' does not belong to restaurant '" + restaurant.getName() + "'");
            }

            BigDecimal subtotal = menuItem.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .menuItemId(menuItem.getId())
                    .itemName(menuItem.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .subtotal(subtotal)
                    .specialInstructions(itemReq.getSpecialInstructions())
                    .build();

            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        OrderEntity savedOrder = orderRepository.save(order);

        // Publish OrderPlacedEvent so Delivery Service creates a delivery asynchronously
        rabbitTemplate.convertAndSend(RabbitMQConfig.APP_EXCHANGE, OrderRoutingKey.ORDER_PLACED.getRoutingKey(), savedOrder);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return enrichWithDeliveryInfo(OrderResponse.fromEntity(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(String username) {
        CustomerResponse customer = customerService.findEntityByUsername(username);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(OrderResponse::fromEntity).map(this::enrichWithDeliveryInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(Long restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId)
                .stream().map(OrderResponse::fromEntity).map(this::enrichWithDeliveryInfo).toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderEntity.OrderStatus newStatus = OrderEntity.OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(newStatus);

        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String username) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        CustomerResponse customer = customerService.findEntityByUsername(username);

        if (!order.getCustomerId().equals(customer.getId())) {
            throw new UnauthorizedException("You can only cancel your own orders");
        }

        if (order.getStatus() != OrderEntity.OrderStatus.PLACED
                && order.getStatus() != OrderEntity.OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderEntity.OrderStatus.CANCELLED);

        // Publish cancellation event so Delivery Service can cancel the delivery asynchronously
        rabbitTemplate.convertAndSend(RabbitMQConfig.APP_EXCHANGE,
                OrderRoutingKey.ORDER_DELETED.getRoutingKey(), OrderResponse.fromEntity(order));

        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    /**
     * Enriches an OrderResponse with delivery information fetched from the Delivery Service.
     * Uses a try-catch so that a Delivery Service outage does not break order retrieval.
     */
    private OrderResponse enrichWithDeliveryInfo(OrderResponse response) {
        try {
            DeliveryInfoResponse delivery = deliveryService.getByOrderId(response.getId());
            if (delivery != null) {
                response.setDeliveryStatus(delivery.getStatus());
                response.setDriverName(delivery.getDriverName());
                response.setDriverPhone(delivery.getDriverPhone());
            }
        } catch (Exception e) {
            log.warn("Could not fetch delivery info for order {}: {}", response.getId(), e.getMessage());
        }
        return response;
    }
}
