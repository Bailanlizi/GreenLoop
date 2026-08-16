package com.campus.trade.integration;

import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.service.FinanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    @BeforeEach
    void setUp() {
        clearTestData();
        insertUser(BUYER_ID, "finance-buyer");
        insertUser(SELLER_ID, "finance-seller");
    }

    @Test
    void concurrentSameRequestProducesOnePaymentFreezeAndFlow() throws Exception {
        createOrder("910001", "920001", new BigDecimal("60.00"));
        rechargeBuyer(new BigDecimal("100.00"), "recharge-same-request");

        List<Boolean> outcomes = concurrently(50, () -> pay("920001", "same-payment-request"));

        assertEquals(50, outcomes.size());
        assertEquals(1, count("SELECT COUNT(*) FROM payment_order"));
        assertEquals(1, count("SELECT COUNT(*) FROM account_freeze_record"));
        assertEquals(1, count("SELECT COUNT(*) FROM account_flow WHERE business_type = 'PAYMENT_FREEZE'"));
        assertEquals(new BigDecimal("40.00"), decimal("SELECT available_balance FROM account WHERE user_id = " + BUYER_ID));
        assertEquals(new BigDecimal("60.00"), decimal("SELECT frozen_balance FROM account WHERE user_id = " + BUYER_ID));
    }

    @Test
    void concurrentDifferentOrdersCannotOverdrawBuyerBalance() throws Exception {
        rechargeBuyer(new BigDecimal("100.00"), "recharge-overdraw");
        for (int i = 0; i < 10; i++) {
            createOrder(String.valueOf(910100 + i), String.valueOf(920100 + i), new BigDecimal("30.00"));
        }
        AtomicInteger nextOrder = new AtomicInteger(920100);

        List<Boolean> outcomes = concurrently(10, () -> {
            String orderId = String.valueOf(nextOrder.getAndIncrement());
            return pay(orderId, "different-request-" + orderId);
        });

        BigDecimal available = decimal("SELECT available_balance FROM account WHERE user_id = " + BUYER_ID);
        BigDecimal frozen = decimal("SELECT frozen_balance FROM account WHERE user_id = " + BUYER_ID);
        BigDecimal frozenPayments = decimal("SELECT COALESCE(SUM(amount), 0) FROM payment_order WHERE status = 'FROZEN'");
        assertTrue(available.signum() >= 0, "available balance must not be negative");
        assertTrue(frozen.signum() >= 0, "frozen balance must not be negative");
        assertTrue(frozenPayments.compareTo(new BigDecimal("100.00")) <= 0, "successful freezes must not exceed initial balance");
        assertEquals(frozen, frozenPayments);
        long successCount = outcomes.stream().filter(Boolean::booleanValue).count();
        assertTrue(successCount > 0 && successCount <= 3,
                "optimistic locking may reject a competing payment, but it must not allow more than three freezes");
    }

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

    private void rechargeBuyer(BigDecimal amount, String requestId) {
        RechargeRequest request = new RechargeRequest();
        request.setAmount(amount);
        request.setRequestId(requestId);
        financeService.recharge(BUYER_ID, request);
    }

    private List<Boolean> concurrently(int workers, Callable<Boolean> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
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
            ready.await();
            start.countDown();
            List<Boolean> outcomes = new ArrayList<>();
            for (Future<Boolean> future : futures) outcomes.add(future.get());
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

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

    private void clearTestData() {
        jdbc.update("DELETE FROM account_flow");
        jdbc.update("DELETE FROM settlement_order");
        jdbc.update("DELETE FROM refund_order");
        jdbc.update("DELETE FROM account_freeze_record");
        jdbc.update("DELETE FROM payment_order");
        jdbc.update("DELETE FROM recharge_order");
        jdbc.update("DELETE FROM account");
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM product");
        jdbc.update("DELETE FROM user");
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private BigDecimal decimal(String sql) { return jdbc.queryForObject(sql, BigDecimal.class); }
}
