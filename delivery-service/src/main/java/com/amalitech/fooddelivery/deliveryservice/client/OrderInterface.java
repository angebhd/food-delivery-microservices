package com.amalitech.fooddelivery.deliveryservice.client;

import com.amalitech.fooddelivery.deliveryservice.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("ORDER-SERVICE")
public interface OrderInterface {

  @GetMapping("/api/orders/{id}")
  OrderResponse getById(@PathVariable Long id);
}
