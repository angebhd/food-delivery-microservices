package com.amalitech.fooddelivery.apigateway.client;
import com.amalitech.fooddelivery.apigateway.dto.CustomerDTO;
import com.amalitech.fooddelivery.apigateway.dto.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
/**
 * Fallback factory for the Customer Service Feign client used by the API Gateway.
 * When the Customer Service is DOWN, authentication operations fail gracefully
 * with a clear business error instead of raw timeout or stacktrace.
 */
@Slf4j
@Component
public class CustomerInterfaceFallbackFactory implements FallbackFactory<CustomerInterface> {
    @Override
    public CustomerInterface create(Throwable cause) {
        return new CustomerInterface() {
            @Override
            public CustomerDTO register(RegisterRequest request) {
                log.error("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot register user={}. Cause: {}", request.getUsername(), cause.getMessage());
                throw new IllegalStateException("Registration is temporarily unavailable. Please try again later.");
            }
            @Override
            public CustomerDTO getByUsername(String username) {
                log.error("Circuit breaker activated: Customer Service is unavailable. "
                        + "Cannot authenticate user={}. Cause: {}", username, cause.getMessage());
                throw new IllegalStateException("Authentication is temporarily unavailable. Please try again later.");
            }
        };
    }
}
