package com.amalitech.fooddelivery.deliveryservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConfig {
  public static final String APP_EXCHANGE = "app.exchange";

  @Bean
  public TopicExchange exchange() {
    return new TopicExchange(APP_EXCHANGE);
  }

  @Bean
  public MessageConverter jsonConverter() {
    return new JacksonJsonMessageConverter();
  }

}
