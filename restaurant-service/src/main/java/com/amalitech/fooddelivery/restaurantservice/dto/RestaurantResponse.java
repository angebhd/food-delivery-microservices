package com.amalitech.fooddelivery.restaurantservice.dto;

import com.amalitech.fooddelivery.restaurantservice.entity.RestaurantEntity;
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

    // Owner info enriched via Feign call to Customer Service in the service layer
    private Long ownerId;
    private String ownerName;

    public static RestaurantResponse fromEntity(RestaurantEntity r) {
        RestaurantResponse dto = new RestaurantResponse();
        dto.setId(r.getId());
        dto.setName(r.getName());
        dto.setDescription(r.getDescription());
        dto.setCuisineType(r.getCuisineType());
        dto.setAddress(r.getAddress());
        dto.setCity(r.getCity());
        dto.setPhone(r.getPhone());
        dto.setActive(r.isActive());
        dto.setRating(r.getRating());
        dto.setEstimatedDeliveryMinutes(r.getEstimatedDeliveryMinutes());
        dto.setMenuItemCount(r.getMenuItems() != null ? r.getMenuItems().size() : 0);
        dto.setOwnerId(r.getOwnerId());
        // ownerName is enriched via Feign call in the service layer
        return dto;
    }
}
