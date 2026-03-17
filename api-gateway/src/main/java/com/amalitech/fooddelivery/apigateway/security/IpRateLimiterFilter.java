package com.amalitech.fooddelivery.apigateway.security;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // runs before all other filters — truly global
public class IpRateLimiterFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(IpRateLimiterFilter.class);

  private final RateLimiterRegistry rateLimiterRegistry;

  public IpRateLimiterFilter(RateLimiterRegistry rateLimiterRegistry) {
    this.rateLimiterRegistry = rateLimiterRegistry;
  }


  @Override
  public void doFilter(ServletRequest servletRequest,
                       ServletResponse servletResponse,
                       FilterChain chain) throws IOException, ServletException {

    HttpServletRequest request   = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    String clientIp = resolve(request);

    // Each IP gets its own RateLimiter instance (created lazily, cached by registry)
    RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(clientIp);

    try {
      RateLimiter.waitForPermission(rateLimiter); // throws RequestNotPermitted when limit hit
      chain.doFilter(request, response);
    } catch (RequestNotPermitted ex) {
      log.warn("Rate limit exceeded for IP: {}", clientIp);
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response.getWriter().write("""
                    {"error": "Too Many Requests", "message": "Rate limit exceeded. Please slow down."}
                    """);
    }
  }
  private String resolve(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip != null && !ip.isBlank()) {
      // X-Forwarded-For can be a comma-separated chain; take the first (original client)
      ip = ip.split(",")[0].trim();
    } else {
      ip = request.getRemoteAddr();
    }
    return ip;
  }
}
