package com.amalitech.fooddelivery.restaurantservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("CUSTOMER-SERVICE")
public interface CustomerInterface {
}
