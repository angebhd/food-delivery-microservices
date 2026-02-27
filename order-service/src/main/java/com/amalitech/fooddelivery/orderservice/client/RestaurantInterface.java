package com.amalitech.fooddelivery.orderservice.client;

import com.amalitech.fooddelivery.orderservice.dto.MenuItemResponse;
import com.amalitech.fooddelivery.orderservice.dto.RestaurantResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("RESTAURANT-SERVICE")
public interface RestaurantInterface {
  @GetMapping("/api/restaurants/{id}")
  RestaurantResponse findEntityById(@PathVariable Long id);

  @GetMapping("/api/restaurants/menu/{id}")
  MenuItemResponse getMenuItemById(@PathVariable Long id);
}
