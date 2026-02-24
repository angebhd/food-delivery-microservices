package com.amalitech.fooddelivery.orderservice.repository;

import com.amalitech.fooddelivery.orderservice.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    // MONOLITH: cross-domain queries joining Order with Customer and Restaurant
    List<OrderEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId); // TODO
    List<OrderEntity> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<OrderEntity> findByStatus(OrderEntity.OrderStatus status);
}
