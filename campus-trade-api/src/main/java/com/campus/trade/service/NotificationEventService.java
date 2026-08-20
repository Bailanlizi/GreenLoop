package com.campus.trade.service;

import com.campus.trade.entity.OutboxEvent;
import com.campus.trade.mapper.OutboxEventMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** 交易领域统一从此处发出通知，避免资金服务直接依赖通知存储实现。 */
@Service
public class NotificationEventService {
    private final NotificationService notificationService;
    private final OutboxEventMapper outboxEventMapper;
    private final boolean asyncEnabled;
    public NotificationEventService(NotificationService notificationService, OutboxEventMapper outboxEventMapper,
                                    @Value("${notifications.async-enabled:false}") boolean asyncEnabled) {
        this.notificationService = notificationService; this.outboxEventMapper = outboxEventMapper; this.asyncEnabled = asyncEnabled;
    }
    public void order(String recipientId, String type, String content, String orderId) { publish(recipientId, type, content, orderId, "ORDER"); }
    public void product(String recipientId, String type, String content, String productId) { publish(recipientId, type, content, productId, "PRODUCT"); }
    public void publish(String recipientId, String type, String content, String relatedId, String relatedType) {
        if (!asyncEnabled) { notificationService.createNotification(recipientId, type, content, relatedId, relatedType, null); return; }
        OutboxEvent event = new OutboxEvent(); event.setEventId(UUID.randomUUID().toString()); event.setEventType(type);
        event.setRecipientId(recipientId); event.setContent(content); event.setRelatedId(relatedId); event.setRelatedType(relatedType);
        outboxEventMapper.insert(event);
    }
}
