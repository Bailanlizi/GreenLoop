package com.campus.trade.config;

import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.service.OrderExpirationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@Component
@ConditionalOnProperty(name = "orders.payment-expiration-enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationScheduler {
    private final OrderMapper orderMapper;
    private final OrderExpirationService expirationService;

    public OrderExpirationScheduler(OrderMapper orderMapper, OrderExpirationService expirationService) {
        this.orderMapper = orderMapper;
        this.expirationService = expirationService;
    }

    @Scheduled(fixedDelayString = "${orders.payment-expiration-scan-ms:60000}")
    public void cancelExpiredOrders() {
        for (String orderId : orderMapper.findExpiredPendingPaymentIds(100)) {
            try {
                expirationService.expirePendingOrder(orderId);
            } catch (RuntimeException ex) {
                log.error("Failed to expire order {}", orderId, ex);
            }
        }
    }
}
