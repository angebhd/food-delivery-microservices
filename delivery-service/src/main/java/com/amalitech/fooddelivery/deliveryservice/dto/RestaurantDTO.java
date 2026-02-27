package com.amalitech.fooddelivery.deliveryservice.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantDTO {
  private String name;
  private String address;
}
