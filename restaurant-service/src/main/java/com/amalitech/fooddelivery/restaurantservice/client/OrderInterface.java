package com.amalitech.fooddelivery.restaurantservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("ORDER-SERVICE")
public interface OrderInterface {
}
