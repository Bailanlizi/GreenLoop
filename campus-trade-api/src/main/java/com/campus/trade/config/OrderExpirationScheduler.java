package com.campus.trade.config;

import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.service.OrderExpirationService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单支付超时扫描（分布式安全版）。
 *
 * 原实现为 @Scheduled 单机扫描：部署多实例时每个实例都会执行同一批
 * 超时订单（虽然 expirePendingOrder 内部有 CAS + 行锁兜底，幂等安全，
 * 但会造成无效扫描与不必要的数据库压力）。
 *
 * 改造后：扫描前先抢 Redisson 分布式锁，抢到锁的实例才执行本周期扫描，
 * 其余实例直接跳过——多实例下同一时刻只有一个执行者。
 * - tryLock(waitTime=0)：抢不到立即返回，不排队、不阻塞调度线程；
 * - 不显式传 leaseTime：走 Redisson watch dog，锁持有期间自动续期，
 *   业务执行完释放；若持有锁的实例崩溃，watchdog 停止续期，锁在
 *   30s 后自动过期，下一周期其它实例可正常接管（无死锁）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "orders.payment-expiration-enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationScheduler {

    /** 分布式锁 key：与缓存同前缀，便于统一排查。 */
    private static final String SCAN_LOCK_KEY = "greenloop:lock:order-expiration-scan";

    private final OrderMapper orderMapper;
    private final OrderExpirationService expirationService;
    private final RedissonClient redissonClient;

    public OrderExpirationScheduler(OrderMapper orderMapper, OrderExpirationService expirationService,
                                    RedissonClient redissonClient) {
        this.orderMapper = orderMapper;
        this.expirationService = expirationService;
        this.redissonClient = redissonClient;
    }

    @Scheduled(fixedDelayString = "${orders.payment-expiration-scan-ms:60000}")
    public void cancelExpiredOrders() {
        RLock lock = redissonClient.getLock(SCAN_LOCK_KEY);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("订单过期扫描获取分布式锁被中断，跳过本周期");
            return;
        }
        if (!acquired) {
            log.debug("订单过期扫描已被其它实例持有，跳过本周期");
            return;
        }

        try {
            for (String orderId : orderMapper.findExpiredPendingPaymentIds(100)) {
                try {
                    expirationService.expirePendingOrder(orderId);
                } catch (RuntimeException ex) {
                    log.error("Failed to expire order {}", orderId, ex);
                }
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
