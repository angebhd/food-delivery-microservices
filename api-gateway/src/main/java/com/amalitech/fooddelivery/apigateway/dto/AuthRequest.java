package com.amalitech.fooddelivery.apigateway.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;

    private String firstName;
    private String lastName;
    private String phone;
    private String deliveryAddress;
    private String city;

}
