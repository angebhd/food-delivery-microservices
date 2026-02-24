package com.amalitech.fooddelivery.deliveryservice.repository;

import com.amalitech.fooddelivery.deliveryservice.entity.DeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {
    Optional<DeliveryEntity> findByOrderId(Long orderId);
    List<DeliveryEntity> findByStatus(DeliveryEntity.DeliveryStatus status);
    List<DeliveryEntity> findByDriverNameIgnoreCase(String driverName);
}
