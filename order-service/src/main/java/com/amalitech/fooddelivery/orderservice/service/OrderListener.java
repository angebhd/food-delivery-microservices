package com.amalitech.fooddelivery.orderservice.service;

import com.amalitech.fooddelivery.orderservice.config.OrderQueueConfig;
import com.amalitech.fooddelivery.orderservice.dto.DeliveryUpdateEvent;
import com.amalitech.fooddelivery.orderservice.entity.OrderEntity;
import com.amalitech.fooddelivery.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens for delivery status update events from the Delivery Service via RabbitMQ.
 * Updates the corresponding order status based on the delivery lifecycle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderListener {

  private final OrderRepository orderRepository;

  @RabbitListener(queues = OrderQueueConfig.QUEUE)
  @Transactional
  public void handleDeliveryUpdate(DeliveryUpdateEvent event) {
    log.info("Received delivery update for order {}: status={}", event.getOrderId(), event.getStatus());

    orderRepository.findById(event.getOrderId()).ifPresentOrElse(order -> {
      switch (event.getStatus().toUpperCase()) {
        case "CONFIRMED" -> order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        case "ASSIGNED" -> order.setStatus(OrderEntity.OrderStatus.CONFIRMED);
        case "PICKED_UP" -> order.setStatus(OrderEntity.OrderStatus.OUT_FOR_DELIVERY);
        case "IN_TRANSIT" -> order.setStatus(OrderEntity.OrderStatus.OUT_FOR_DELIVERY);
        case "DELIVERED" -> order.setStatus(OrderEntity.OrderStatus.DELIVERED);
        case "FAILED" -> order.setStatus(OrderEntity.OrderStatus.CANCELLED);
        default -> log.warn("Unknown delivery status: {}", event.getStatus());
      }
      orderRepository.save(order);
      log.info("Order {} status updated to {}", order.getId(), order.getStatus());
    }, () -> log.warn("Order not found for delivery update: orderId={}", event.getOrderId()));
  }
}
