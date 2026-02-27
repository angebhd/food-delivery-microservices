package com.amalitech.fooddelivery.deliveryservice.service;

import com.amalitech.fooddelivery.deliveryservice.config.DeliveryQueueConfig;
import com.amalitech.fooddelivery.deliveryservice.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryListener {

  private final DeliveryService deliveryService;

  @RabbitListener(queues = DeliveryQueueConfig.QUEUE)
  public void handleOrderCreated(OrderResponse event) {

    log.info("Creating delivery for order {}",event.getId());
    deliveryService.createDeliveryForOrder(event);

  }
}