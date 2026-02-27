package com.amalitech.fooddelivery.deliveryservice.dto;

public enum DeliveryRoutingKey {
  DELIVERY_UPDATE("delivery.update");
  private final String routingKey;

  DeliveryRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getRoutingKey() {
    return routingKey;
  }
}
