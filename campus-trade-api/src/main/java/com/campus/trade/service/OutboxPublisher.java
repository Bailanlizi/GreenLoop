package com.campus.trade.service;

import com.campus.trade.entity.OutboxEvent;
import com.campus.trade.mapper.OutboxEventMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "notifications.async-enabled", havingValue = "true")
public class OutboxPublisher {
    private final OutboxEventMapper mapper; private final RabbitTemplate rabbitTemplate; private final String exchange; private final int maxRetries;
    public OutboxPublisher(OutboxEventMapper mapper, RabbitTemplate rabbitTemplate, @Value("${notifications.rabbit.exchange}") String exchange, @Value("${notifications.outbox-max-retries:3}") int maxRetries) { this.mapper=mapper; this.rabbitTemplate=rabbitTemplate; this.exchange=exchange; this.maxRetries=maxRetries; }
    @Scheduled(fixedDelayString = "${notifications.outbox-publish-ms:5000}")
    public void publishPending() {
        for (OutboxEvent event : mapper.findPublishable(100)) {
            try {
                CorrelationData correlation = new CorrelationData(event.getEventId());
                rabbitTemplate.convertAndSend(exchange, "notification", event, correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (confirm == null || !confirm.isAck()) throw new IllegalStateException(confirm == null ? "publisher confirm timeout" : confirm.getReason());
                mapper.markPublished(event.getId());
            }
            catch (Exception ex) { if (ex instanceof InterruptedException) Thread.currentThread().interrupt(); int attempts=(event.getRetryCount()==null?0:event.getRetryCount())+1; long delay=Math.min(300000L, 1000L << Math.min(attempts, 8)); mapper.markRetry(event.getId(), attempts >= maxRetries ? "DEAD" : "PENDING", new Date(System.currentTimeMillis()+delay), abbreviate(ex.getMessage())); }
        }
    }
    private String abbreviate(String message) { return message == null ? "publish failed" : message.substring(0, Math.min(message.length(), 500)); }
}
