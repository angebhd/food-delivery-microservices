package com.amalitech.fooddelivery.customerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long customerId;
    private String username;
    private String role;
}
