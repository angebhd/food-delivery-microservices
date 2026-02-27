package com.amalitech.fooddelivery.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — part of the Order domain.
 *
 * MONOLITH PROBLEM: Direct @ManyToOne to both Customer and
 * Restaurant entities, plus @OneToOne to Delivery. This creates
 * tight coupling across three domain boundaries.
 *
 * In microservices:
 *  - Store customerId, restaurantId as Long values
 *  - Fetch customer/restaurant details via Feign clients
 *  - Delivery assignment via event (OrderPlacedEvent)
 */

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@AllArgsConstructor @NoArgsConstructor
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false)
  private BigDecimal totalAmount;

  private BigDecimal deliveryFee;

  private String deliveryAddress;

  private String specialInstructions;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
  private LocalDateTime estimatedDeliveryTime;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItemEntity> items = new ArrayList<>();

  // ---- CROSS-DOMAIN REFERENCES (IDs + snapshots captured at order time) ----

  private Long customerId;

  /** Snapshot of the customer name captured at order placement time. */
  private String customerName;

  private Long restaurantId;

  /** Snapshot of the restaurant name captured at order placement time. */
  private String restaurantName;

  /** Snapshot of the restaurant address captured at order placement time. */
  private String restaurantAddress;

  private Long deliveryId;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) status = OrderStatus.PLACED;
    if (deliveryFee == null) deliveryFee = new BigDecimal("2.99");
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
  }
}
