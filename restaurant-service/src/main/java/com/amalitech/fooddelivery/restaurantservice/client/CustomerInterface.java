package com.amalitech.fooddelivery.restaurantservice.client;

import com.amalitech.fooddelivery.restaurantservice.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient("CUSTOMER-SERVICE")
public interface CustomerInterface {
  @GetMapping("api/customers/username/{username}")
  CustomerResponse findEntityByUsername(@PathVariable String username);

  @GetMapping("api/customers/id/{id}")
  CustomerResponse getById(@PathVariable Long id);

  @PutMapping("api/customers/make-restaurant-owner")
  ResponseEntity<CustomerResponse> makeRestaurantOwner(Authentication auth);
}
