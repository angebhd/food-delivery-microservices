package com.amalitech.fooddelivery.deliveryservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor @NoArgsConstructor
public class OrderDTO {
  private Long id;
  private  String address;
  private RestaurantDTO restaurant;
  private CustomerResponse customer;



}
