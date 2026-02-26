package com.amalitech.fooddelivery.apigateway.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDTO {
  private Long id;
  private String username;
  private String role;
  private String password;
}

