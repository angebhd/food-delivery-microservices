package com.amalitech.fooddelivery.deliveryservice.client;


import com.amalitech.fooddelivery.deliveryservice.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient("CUSTOMER-SERVICE")
public interface CustomerInterface {

  @GetMapping("api/customers/username/{username}")
  CustomerResponse findEntityByUsername(@PathVariable String username);

  @GetMapping("api/customers/id/{id}")
  CustomerResponse getById(@PathVariable Long id);
}
