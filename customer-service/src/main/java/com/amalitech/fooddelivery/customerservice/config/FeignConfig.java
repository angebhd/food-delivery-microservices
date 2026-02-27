package com.amalitech.fooddelivery.customerservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignConfig {

  @Bean
  public RequestInterceptor headerPropagationInterceptor() {
    return (RequestTemplate template) -> {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        String authUser = request.getHeader("X-Auth-User");
        if (authUser != null) {
          template.header("X-Auth-User", authUser);
        }

        String authRole = request.getHeader("X-Auth-Role");
        if (authRole != null) {
          template.header("X-Auth-Role", authRole);
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
          template.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

      }
    };
  }
}