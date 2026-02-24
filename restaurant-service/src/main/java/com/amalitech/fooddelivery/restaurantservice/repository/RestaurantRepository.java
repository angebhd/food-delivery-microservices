package com.amalitech.fooddelivery.restaurantservice.repository;

import com.amalitech.fooddelivery.restaurantservice.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
    List<RestaurantEntity> findByActiveTrue();
    List<RestaurantEntity> findByCityIgnoreCaseAndActiveTrue(String city);
    List<RestaurantEntity> findByCuisineTypeIgnoreCaseAndActiveTrue(String cuisineType);
    // MONOLITH: cross-domain query using Customer (owner) relationship
    List<RestaurantEntity> findByOwnerId(Long ownerId);
}
