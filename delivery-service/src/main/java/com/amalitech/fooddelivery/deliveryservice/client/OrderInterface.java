package com.amalitech.fooddelivery.deliveryservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("ORDER-SERVICE")
public interface OrderInterface {
}
