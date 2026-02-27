package com.amalitech.fooddelivery.deliveryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DeliveryUpdateEvent {
  private Long orderId;
  private String status;
}
