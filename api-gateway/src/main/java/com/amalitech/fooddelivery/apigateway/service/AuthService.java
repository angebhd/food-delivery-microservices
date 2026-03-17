package com.amalitech.fooddelivery.apigateway.service;

import com.amalitech.fooddelivery.apigateway.client.CustomerInterface;
import com.amalitech.fooddelivery.apigateway.dto.AuthRequest;
import com.amalitech.fooddelivery.apigateway.dto.AuthResponse;
import com.amalitech.fooddelivery.apigateway.dto.CustomerDTO;
import com.amalitech.fooddelivery.apigateway.dto.RegisterRequest;
import com.amalitech.fooddelivery.apigateway.exception.ServiceUnavailableException;
import com.amalitech.fooddelivery.apigateway.exception.UnauthorizedException;
import com.amalitech.fooddelivery.apigateway.security.JwtUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {
  private final CustomerInterface customerService;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

    @CircuitBreaker(name = "customerService", fallbackMethod = "registerFallback")
    public AuthResponse register(RegisterRequest request) {
        // Call the customer service to create a new customer
        CustomerDTO customer = customerService.register(request);

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole());
        return new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getRole());
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "loginFallback")
    public AuthResponse login(AuthRequest request) {
      // Call the customer service to get the customer by username
        CustomerDTO customer = customerService.getByUsername(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(customer.getUsername(), customer.getRole());
        return new AuthResponse(token, customer.getId(), customer.getUsername(), customer.getRole());
    }

    private AuthResponse registerFallback(RegisterRequest request, Throwable t) {
        log.error("Circuit breaker triggered for registration. Customer Service is unavailable. User: {}, Cause: {}",
                request.getUsername(), t.getMessage());
        throw new ServiceUnavailableException("Registration is temporarily unavailable. Please try again later.");
    }

    private AuthResponse loginFallback(AuthRequest request, Throwable t) {
        log.error("Circuit breaker triggered for login. Customer Service is unavailable. User: {}, Cause: {}",
                request.getUsername(), t.getMessage());
        throw new ServiceUnavailableException("Login is temporarily unavailable. Please try again later.");
    }
}
