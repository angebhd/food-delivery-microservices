package com.amalitech.fooddelivery.orderservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderQueueConfig {

  public static final String QUEUE = "order.queue";

  @Bean
  public Queue orderQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  public Binding orderBinding(Queue orderQueue, TopicExchange exchange) {
    return BindingBuilder
            .bind(orderQueue)
            .to(exchange)
            .with("delivery.*");
  }

}
