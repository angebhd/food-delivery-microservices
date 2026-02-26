package com.amalitech.fooddelivery.customerservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class SecurityContextFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
          throws ServletException, IOException {
    String username = request.getHeader("X-Auth-User");
    log.warn("SecurityContextFilter: Extracted username from header: {}", username);

    if (username != null) {
      List<SimpleGrantedAuthority> authority = request.getHeader("X-Auth-Role") == null ?  Collections.emptyList() : Stream.of(request.getHeader("X-Auth-Role").split(",")).map(SimpleGrantedAuthority::new).toList();
      Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authority);
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
  }
}