package com.amalitech.fooddelivery.restaurantservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(nullable = false)
  private BigDecimal price;

  private String category;

  private boolean available;

  private String imageUrl;

  // ---- SAME-DOMAIN RELATIONSHIP (this is fine) ----

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "restaurant_id", nullable = false)
  private RestaurantEntity restaurant;

  @PrePersist
  protected void onCreate() {
    available = true;
  }
}
