package com.campus.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "notifications.async-enabled", havingValue = "true")
public class NotificationRabbitConfig {
    @Bean DirectExchange notificationExchange(@Value("${notifications.rabbit.exchange}") String name) { return new DirectExchange(name, true, false); }
    @Bean DirectExchange notificationDlx(@Value("${notifications.rabbit.exchange}") String name) { return new DirectExchange(name + ".dlx", true, false); }
    @Bean Queue notificationQueue(@Value("${notifications.rabbit.queue}") String name, @Value("${notifications.rabbit.exchange}") String exchange) {
        return QueueBuilder.durable(name).withArgument("x-dead-letter-exchange", exchange + ".dlx").withArgument("x-dead-letter-routing-key", "dead").build();
    }
    @Bean Queue notificationDlq(@Value("${notifications.rabbit.dlq}") String name) { return QueueBuilder.durable(name).build(); }
    @Bean Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) { return BindingBuilder.bind(notificationQueue).to(notificationExchange).with("notification"); }
    @Bean Binding notificationDeadBinding(Queue notificationDlq, DirectExchange notificationDlx) { return BindingBuilder.bind(notificationDlq).to(notificationDlx).with("dead"); }
}
