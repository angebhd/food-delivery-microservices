package com.amalitech.fooddelivery.customerservice.repository;

import com.amalitech.fooddelivery.customerservice.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByUsername(String username);
    Optional<CustomerEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
