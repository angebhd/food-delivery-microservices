package com.amalitech.fooddelivery.orderservice.dto;

public enum OrderRoutingKey {
  ORDER_PLACED("order.placed"),
  ORDER_UPDATED("order.updated"),
  ORDER_DELETED("order.deleted");

  private final String routingKey;

  OrderRoutingKey(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getRoutingKey() {
    return routingKey;
  }
}
