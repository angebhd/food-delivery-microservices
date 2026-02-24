package com.amalitech.fooddelivery.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("CUSTOMER-SERVICE")
public interface CustomerInterface {
}
