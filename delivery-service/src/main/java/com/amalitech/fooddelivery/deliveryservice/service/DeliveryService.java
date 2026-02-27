package com.amalitech.fooddelivery.deliveryservice.service;

import com.amalitech.fooddelivery.deliveryservice.client.CustomerInterface;
import com.amalitech.fooddelivery.deliveryservice.client.OrderInterface;
import com.amalitech.fooddelivery.deliveryservice.config.RabbitMQConfig;
import com.amalitech.fooddelivery.deliveryservice.dto.*;
import com.amalitech.fooddelivery.deliveryservice.entity.DeliveryEntity;
import com.amalitech.fooddelivery.deliveryservice.exception.ResourceNotFoundException;
import com.amalitech.fooddelivery.deliveryservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Delivery Service business logic.
 *
 * In the microservice architecture:
 *  - Subscribes to OrderPlacedEvent via RabbitMQ to create deliveries asynchronously
 *  - Stores orderId as a local reference
 *  - Publishes DeliveryStatusUpdatedEvent when delivery status changes
 *  - Enriches delivery responses with order/customer info via Feign calls
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);
    private final CustomerInterface customerService;
    private final OrderInterface orderService;
    private final RabbitTemplate rabbitTemplate;
    private final DeliveryRepository deliveryRepository;

    // Simulated driver pool — in reality this would be its own service
    private static final String[] DRIVERS = {
            "Carlos Martinez", "Sarah Johnson", "Mike Chen", "Priya Patel", "James Wilson"
    };
    private static final String[] PHONES = {
            "+1-555-0101", "+1-555-0102", "+1-555-0103", "+1-555-0104", "+1-555-0105"
    };


    /**
     * Creates a delivery assignment for an order.
     * Triggered asynchronously by consuming OrderPlacedEvent from RabbitMQ.
     */
    @Transactional
    public void createDeliveryForOrder(OrderResponse order) {
        int driverIndex = (int) (Math.random() * DRIVERS.length);

        DeliveryEntity delivery = DeliveryEntity.builder()
                .orderId(order.getId())
                .status(DeliveryEntity.DeliveryStatus.ASSIGNED)
                .driverName(DRIVERS[driverIndex])
                .driverPhone(PHONES[driverIndex])
                .pickupAddress(order.getRestaurantAddress())
                .deliveryAddress(order.getDeliveryAddress())
                .assignedAt(LocalDateTime.now())
                .build();

        deliveryRepository.save(delivery);

        log.info("NOTIFICATION: Delivery assigned to {} for order #{} — Customer: {}, Restaurant: {}",
                DRIVERS[driverIndex],
                order.getId(),
                order.getCustomerName(),
                order.getRestaurantName());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.APP_EXCHANGE,
                DeliveryRoutingKey.DELIVERY_UPDATE.getRoutingKey(),
                new DeliveryUpdateEvent(delivery.getOrderId(), "CONFIRMED")
        );

    }

    @Transactional(readOnly = true)
    public DeliveryResponse getByOrderId(Long orderId) {
        DeliveryEntity delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "orderId", orderId));
        return enrichWithOrderInfo(DeliveryResponse.fromEntity(delivery));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getById(Long deliveryId) {
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));
        return enrichWithOrderInfo(DeliveryResponse.fromEntity(delivery));
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getByStatus(String status) {
        DeliveryEntity.DeliveryStatus deliveryStatus = DeliveryEntity.DeliveryStatus.valueOf(status.toUpperCase());
        return deliveryRepository.findByStatus(deliveryStatus)
                .stream().map(DeliveryResponse::fromEntity).map(this::enrichWithOrderInfo).toList();
    }

    @Transactional
    public DeliveryResponse updateStatus(Long deliveryId, String status) {
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));

        DeliveryEntity.DeliveryStatus newStatus = DeliveryEntity.DeliveryStatus.valueOf(status.toUpperCase());
        delivery.setStatus(newStatus);

        switch (newStatus) {
            case PICKED_UP -> delivery.setPickedUpAt(LocalDateTime.now());
            case DELIVERED -> {
                delivery.setDeliveredAt(LocalDateTime.now());
                // Produce DeliveryUpdateEvent to RabbitMQ so Order Service can update order status and notify customer
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.APP_EXCHANGE,
                        DeliveryRoutingKey.DELIVERY_UPDATE.getRoutingKey(),
                        new DeliveryUpdateEvent(delivery.getOrderId(), newStatus.name())
                );

            }
        }

        log.info("NOTIFICATION: Delivery #{} status changed to {} — OrderId: {}", deliveryId, newStatus, delivery.getOrderId());

        return DeliveryResponse.fromEntity(deliveryRepository.save(delivery));
    }

    @Transactional
    public void cancelDelivery(Long deliveryId) {
        DeliveryEntity delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", deliveryId));
        delivery.setStatus(DeliveryEntity.DeliveryStatus.FAILED);
        deliveryRepository.save(delivery);
        log.info("NOTIFICATION: Delivery #{} cancelled", deliveryId);
    }

    /**
     * Enriches a DeliveryResponse with order, customer, and restaurant information
     * fetched from the Order Service and Customer Service via Feign.
     * Uses a try-catch so that a downstream service outage does not break delivery retrieval.
     */
    private DeliveryResponse enrichWithOrderInfo(DeliveryResponse response) {
        if (response.getOrderId() == null) {
            return response;
        }
        try {
            OrderResponse order = orderService.getById(response.getOrderId());
            if (order != null) {
                response.setOrderStatus(order.getStatus());
                response.setCustomerId(order.getCustomerId());
                response.setCustomerName(order.getCustomerName());
                response.setRestaurantName(order.getRestaurantName());
            }
        } catch (Exception e) {
            log.warn("Could not fetch order info for delivery {}: {}", response.getId(), e.getMessage());
        }
        return response;
    }
}
