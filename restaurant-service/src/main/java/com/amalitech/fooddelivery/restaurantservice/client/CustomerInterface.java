package com.amalitech.fooddelivery.restaurantservice.client;

import com.amalitech.fooddelivery.restaurantservice.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "CUSTOMER-SERVICE", fallbackFactory = CustomerInterfaceFallbackFactory.class)
public interface CustomerInterface {
  @GetMapping("api/customers/username/{username}")
  CustomerResponse findEntityByUsername(@PathVariable String username);

  @GetMapping("api/customers/id/{id}")
  CustomerResponse getById(@PathVariable Long id);

  @PutMapping("api/customers/make-restaurant-owner")
  CustomerResponse makeRestaurantOwner();
}
