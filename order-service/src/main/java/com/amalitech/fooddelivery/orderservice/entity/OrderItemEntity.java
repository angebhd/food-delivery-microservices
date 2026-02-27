package com.amalitech.fooddelivery.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@AllArgsConstructor @NoArgsConstructor
public class OrderItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private BigDecimal subtotal;

  private String specialInstructions;

  // ---- SAME-DOMAIN RELATIONSHIP (fine for Order Service) ----

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private OrderEntity order;

  // ---- CROSS-DOMAIN REFERENCES (stored as snapshots at order time) ----

  private Long menuItemId;

  /** Snapshot of the menu item name captured at order placement time. */
  private String itemName;
}
