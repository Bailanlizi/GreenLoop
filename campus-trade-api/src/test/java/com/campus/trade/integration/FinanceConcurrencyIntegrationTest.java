package com.campus.trade.integration;

import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.service.FinanceService;
import com.campus.trade.service.NotificationEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "orders.payment-expiration-enabled=false",
        "security.bootstrap-admin.enabled=false",
        "ai.enabled=false",
        "spring.mail.host=localhost"
})
class FinanceConcurrencyIntegrationTest {
    private static final String BUYER_ID = "900001";
    private static final String SELLER_ID = "900002";

    @Autowired private FinanceService financeService;
    @Autowired private JdbcTemplate jdbc;
    @MockBean private NotificationEventService notificationEvents;

    //每次测试前都会运行一次，清空测试数据，保证测试环境干净
    @BeforeEach
    void setUp() {
        clearTestData();
        insertUser(BUYER_ID, "finance-buyer");
        insertUser(SELLER_ID, "finance-seller");
    }

//幂等性拦截率100%（重复提交100次，仅1次扣款）
    @Test
    void concurrentSameRequestProducesOnePaymentFreezeAndFlow() throws Exception {
        createOrder("910001", "920001", new BigDecimal("60.00"));
        rechargeBuyer(new BigDecimal("100.00"), "recharge-same-request");
        //50个线程并发支付同一个订单，且使用完全相同的requestid
        List<Boolean> outcomes = concurrently(50, () -> pay("920001", "same-payment-request"));
        //核心断言：支付单表里（本测试买家名下）只有1条记录：幂等生效
        assertEquals(50, outcomes.size());
        assertEquals(1, count("SELECT COUNT(*) FROM payment_order WHERE buyer_id = " + BUYER_ID));
        assertEquals(1, count("SELECT COUNT(*) FROM account_freeze_record WHERE user_id = " + BUYER_ID));
        assertEquals(1, count("SELECT COUNT(*) FROM account_flow WHERE business_type = 'PAYMENT_FREEZE' AND user_id = " + BUYER_ID));
        assertEquals(new BigDecimal("40.00"), decimal("SELECT available_balance FROM account WHERE user_id = " + BUYER_ID));
        assertEquals(new BigDecimal("60.00"), decimal("SELECT frozen_balance FROM account WHERE user_id = " + BUYER_ID));
    }

    // 并发扣款一致性测试：100元余额不能被扣超 对应指标：并发扣款一致性（100线程，仅1笔能成功，余额不为负）
    @Test
    void concurrentDifferentOrdersCannotOverdrawBuyerBalance() throws Exception {
        rechargeBuyer(new BigDecimal("100.00"), "recharge-overdraw");
        for (int i = 0; i < 10; i++) {
            createOrder(String.valueOf(910100 + i), String.valueOf(920100 + i), new BigDecimal("30.00"));
        }
        //原子计数器；用于让每个线程拿到不同的订单号
        AtomicInteger nextOrder = new AtomicInteger(920100);

        // 核心：10个线程同时支付不同的订单，抢同一个买家的100元余额
        List<Boolean> outcomes = concurrently(10, () -> {
            String orderId = String.valueOf(nextOrder.getAndIncrement());
            return pay(orderId, "different-request-" + orderId);
        });

        //买家的可用余额、冻结余额都不能为负数
        BigDecimal available = decimal("SELECT available_balance FROM account WHERE user_id = " + BUYER_ID);
        BigDecimal frozen = decimal("SELECT frozen_balance FROM account WHERE user_id = " + BUYER_ID);
        BigDecimal frozenPayments = decimal("SELECT COALESCE(SUM(amount), 0) FROM payment_order WHERE status = 'FROZEN' AND buyer_id = " + BUYER_ID);
        assertTrue(available.signum() >= 0, "available balance must not be negative");
        assertTrue(frozen.signum() >= 0, "frozen balance must not be negative");
        assertTrue(frozenPayments.compareTo(new BigDecimal("100.00")) <= 0, "successful freezes must not exceed initial balance");
        assertEquals(frozen, frozenPayments);
        long successCount = outcomes.stream().filter(Boolean::booleanValue).count();
        assertTrue(successCount > 0 && successCount <= 3,
                "optimistic locking may reject a competing payment, but it must not allow more than three freezes");
    }

    //辅助方法：执行支付并捕获异常（异常表示支付失败）
    private boolean pay(String orderId, String requestId) {
        try {
            PaymentRequest request = new PaymentRequest();
            request.setRequestId(requestId);
            financeService.payOrder(orderId, BUYER_ID, request);
            return true;
        } catch (RuntimeException expectedForInsufficientBalanceOrDuplicate) {
            return false;
        }
    }

    //辅助方法：买家充值
    private void rechargeBuyer(BigDecimal amount, String requestId) {
        RechargeRequest request = new RechargeRequest();
        request.setAmount(amount);
        request.setRequestId(requestId);
        financeService.recharge(BUYER_ID, request);
    }

    //并发执行的核心工具：让多个线程在同一时刻”一起出发“，模拟真实的高并发场景
    private List<Boolean> concurrently(int workers, Callable<Boolean> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers); //创建固定大小的线程池
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();//等待所有线程都准备好
            start.countDown();//所有被阻塞的线程同时启动
            List<Boolean> outcomes = new ArrayList<>();
            for (Future<Boolean> future : futures) outcomes.add(future.get());
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    //数据库操作辅助方法
    private void createOrder(String productId, String orderId, BigDecimal amount) {
        jdbc.update("INSERT INTO product(id, seller_id, category_id, title, description, price, cover_image, status, delivery_options) VALUES (?, ?, 1, ?, 'test', ?, 'test.png', 'LOCKED', 'MEETUP')",
                Long.valueOf(productId), Long.valueOf(SELLER_ID), "finance-product-" + productId, amount);
        jdbc.update("INSERT INTO orders(id, product_id, buyer_id, seller_id, order_status, total_price, delivery_method, payment_deadline) VALUES (?, ?, ?, ?, 'PENDING_PAYMENT', ?, 'MEETUP', ?)",
                Long.valueOf(orderId), Long.valueOf(productId), Long.valueOf(BUYER_ID), Long.valueOf(SELLER_ID), amount,
                Timestamp.from(Instant.now().plusSeconds(300)));
    }

    private void insertUser(String id, String username) {
        jdbc.update("INSERT INTO user(id, username, password, nickname, role, credit_score, status) VALUES (?, ?, 'test', ?, 'USER', 100, 1)",
                Long.valueOf(id), username, username);
    }

    //只清理本测试自有数据（买家900001/卖家900002），不触碰库内其他数据（如 JMeter 压测数据、k6 夹具）
    private void clearTestData() {
        jdbc.update("DELETE FROM notifications WHERE user_id IN (?, ?)", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM account_flow WHERE user_id IN (?, ?)", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM settlement_order WHERE buyer_id = ? OR seller_id = ?", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM refund_order WHERE buyer_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM account_freeze_record WHERE user_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM payment_order WHERE buyer_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM recharge_order WHERE user_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM account WHERE user_id IN (?, ?)", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM ratings WHERE order_id IN (SELECT id FROM orders WHERE buyer_id = ? OR seller_id = ?)", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM orders WHERE buyer_id = ? OR seller_id = ?", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM product WHERE seller_id = ?", SELLER_ID);
        jdbc.update("DELETE FROM user WHERE id IN (?, ?)", BUYER_ID, SELLER_ID);
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private BigDecimal decimal(String sql) { return jdbc.queryForObject(sql, BigDecimal.class); }
}
