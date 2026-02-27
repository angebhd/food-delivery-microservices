package com.amalitech.fooddelivery.deliveryservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeliveryQueueConfig {

  public static final String QUEUE = "delivery.queue";

  @Bean
  public Queue deliveryQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  public Binding deliveryBinding(Queue deliveryQueue,
                                 TopicExchange exchange) {
    return BindingBuilder
            .bind(deliveryQueue)
            .to(exchange)
            .with("order.*");
  }
}