package com.amalitech.fooddelivery.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("DELIVERY-SERVICE")
public interface DeliveryInterface {
}
