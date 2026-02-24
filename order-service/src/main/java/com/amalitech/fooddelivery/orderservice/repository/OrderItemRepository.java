package com.amalitech.fooddelivery.orderservice.repository;

import com.amalitech.fooddelivery.orderservice.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
}
