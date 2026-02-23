package com.amalitech.fooddelivery.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Delivery entity — part of the Delivery domain.
 *
 * MONOLITH PROBLEM: Direct @OneToOne to Order entity and
 * delivery assignment happens SYNCHRONOUSLY inside the
 * OrderService.placeOrder() method. This blocks the order
 * response until delivery is assigned.
 *
 * In microservices:
 *  - Store orderId as a Long reference
 *  - Delivery Service subscribes to OrderPlacedEvent via RabbitMQ
 *  - Assignment happens ASYNCHRONOUSLY after the order is confirmed
 *  - Delivery Service publishes DeliveryStatusUpdatedEvent
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
public class DeliveryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  private String driverName;
  private String driverPhone;

  private String pickupAddress;
  private String deliveryAddress;

  private LocalDateTime assignedAt;
  private LocalDateTime pickedUpAt;
  private LocalDateTime deliveredAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private Long orderId;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (status == null) status = DeliveryStatus.PENDING;
  }

  public enum DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED
  }

}
