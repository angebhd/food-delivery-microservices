package com.amalitech.fooddelivery.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("DELIVERY-SERVICE")
public interface DeliveryInterface {

  @GetMapping("/api/deliveries/order/{orderId}")
  DeliveryInfoResponse getByOrderId(@PathVariable Long orderId);
}
