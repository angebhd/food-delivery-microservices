package com.amalitech.fooddelivery.customerservice.controller;

import com.amalitech.fooddelivery.customerservice.dto.CustomerResponse;
import com.amalitech.fooddelivery.customerservice.dto.RegisterRequest;
import com.amalitech.fooddelivery.customerservice.entity.CustomerEntity;
import com.amalitech.fooddelivery.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/create")
    public ResponseEntity<CustomerEntity> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getMyProfile(Authentication auth) {
        log.warn("Fetching profile for user: {}", auth.getName());
        return ResponseEntity.ok(customerService.getProfile(auth.getName()));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<CustomerEntity> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(customerService.findEntityByUsername(username));
    }

    @PutMapping("/me")
    public ResponseEntity<CustomerResponse> updateProfile(
            Authentication auth, @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(auth.getName(), request));
    }

    @PutMapping("/make-restaurant-owner")
    public ResponseEntity<CustomerResponse> makeRestaurantOwner(Authentication auth) {
        return ResponseEntity.ok(customerService.updateProfile(auth.getName()));
    }
}
