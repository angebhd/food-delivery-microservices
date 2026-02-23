package com.amalitech.fooddelivery.customerservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer entity — part of the Customer domain.
 *
 * MONOLITH PROBLEM: Direct @OneToMany relationships to Order and
 * Delivery entities create tight coupling across domains.
 * In microservices, other services should only store customerId
 * and fetch details via REST when needed.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
public class CustomerEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String password;

  private String firstName;
  private String lastName;

  @Column(unique = true)
  private String phone;

  private String deliveryAddress;
  private String city;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private List<Long> orderIds = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum Role {
    CUSTOMER, RESTAURANT_OWNER, ADMIN
  }
}
