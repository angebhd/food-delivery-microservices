package com.amalitech.fooddelivery.orderservice.dto;

import lombok.Data;

@Data
public class RestaurantResponse {
    private Long id;
    private String name;
    private String description;
    private String cuisineType;
    private String address;
    private String city;
    private String phone;
    private boolean active;
    private double rating;
    private int estimatedDeliveryMinutes;
    private int menuItemCount;

    // Owner info
    private Long ownerId;
    private String ownerName;

}
