package com.amalitech.fooddelivery.orderservice.client;


import com.amalitech.fooddelivery.orderservice.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "CUSTOMER-SERVICE", fallbackFactory = CustomerInterfaceFallbackFactory.class)
public interface CustomerInterface {

  @GetMapping("api/customers/username/{username}")
  CustomerResponse findEntityByUsername(@PathVariable String username);

}
