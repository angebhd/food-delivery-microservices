package com.amalitech.fooddelivery.restaurantservice.repository;

import com.amalitech.fooddelivery.restaurantservice.entity.MenuItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItemEntity, Long> {
    List<MenuItemEntity> findByRestaurantIdAndAvailableTrue(Long restaurantId);
    List<MenuItemEntity> findByRestaurantId(Long restaurantId);
    List<MenuItemEntity> findByRestaurantIdAndCategory(Long restaurantId, String category);
}
