package com.amalitech.fooddelivery.restaurantservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity  // Enables @PreAuthorize, etc.
public class SecurityConfig {

  private final SecurityContextFilter securityContextFilter;  // Inject your filter

  public SecurityConfig(SecurityContextFilter securityContextFilter) {
    this.securityContextFilter = securityContextFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)  // Disable if not needed
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(securityContextFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
  }
}
