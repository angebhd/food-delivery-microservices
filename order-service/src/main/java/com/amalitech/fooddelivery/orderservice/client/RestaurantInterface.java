package com.amalitech.fooddelivery.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("RESTAURANT-SERVICE")
public interface RestaurantInterface {
}
