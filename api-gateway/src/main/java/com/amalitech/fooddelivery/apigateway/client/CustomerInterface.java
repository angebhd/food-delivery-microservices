package com.amalitech.fooddelivery.apigateway.client;

import com.amalitech.fooddelivery.apigateway.dto.CustomerDTO;
import com.amalitech.fooddelivery.apigateway.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("CUSTOMER-SERVICE")
public interface CustomerInterface {
  @PostMapping("/api/customers/create")
  CustomerDTO register(@Valid @RequestBody RegisterRequest request);

  @GetMapping("api/customers/username/{username}")
  CustomerDTO getByUsername(@PathVariable String username);


}
