package com.campus.trade.service;

import com.campus.trade.entity.OutboxEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notifications.async-enabled", havingValue = "true")
public class NotificationConsumer {
    private final NotificationService notificationService;
    public NotificationConsumer(NotificationService notificationService) { this.notificationService = notificationService; }
    @RabbitListener(queues = "${notifications.rabbit.queue}")
    public void consume(OutboxEvent event) {
        try { notificationService.createNotification(event.getRecipientId(), event.getEventType(), event.getContent(), event.getRelatedId(), event.getRelatedType(), event.getEventId()); }
        catch (DuplicateKeyException ignored) { /* same source event already stored: idempotent success */ }
    }
}
