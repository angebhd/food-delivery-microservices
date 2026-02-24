package com.amalitech.fooddelivery.customerservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("ORDER-SERVICE")
public interface OrderInterface {
}
