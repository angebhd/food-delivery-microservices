package com.amalitech.fooddelivery.apigateway.service;

import com.amalitech.fooddelivery.apigateway.client.CustomerInterface;
import com.amalitech.fooddelivery.apigateway.dto.AuthRequest;
import com.amalitech.fooddelivery.apigateway.dto.AuthResponse;
import com.amalitech.fooddelivery.apigateway.dto.CustomerDTO;
import com.amalitech.fooddelivery.apigateway.dto.RegisterRequest;
import com.amalitech.fooddelivery.apigateway.exception.UnauthorizedException;
import com.amalitech.fooddelivery.apigateway.security.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
  private final CustomerInterface customerService;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;


    public AuthResponse register(RegisterRequest request) {

      // Call the customer service to create a new customer
        CustomerDTO customer = customerService.register(request);

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole());
        return new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getRole());
    }


    public AuthResponse login(AuthRequest request) {
      // Call the customer service to get the customer by username
        CustomerDTO customer = customerService.getByUsername(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole());
        return new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getRole());
    }


}
