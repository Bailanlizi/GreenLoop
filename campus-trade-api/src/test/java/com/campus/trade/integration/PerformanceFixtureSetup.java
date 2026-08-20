package com.campus.trade.integration;

import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.service.FinanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

/**
 * k6 性能测试夹具准备器（不是自动化测试，不含断言）。
 *
 * 【命名约定】类名刻意不带 "Test" 后缀，Maven Surefire 不会在 mvn test 时自动执行它，
 * 避免误清空 campus_trade_test 库中其他测试数据（如 JMeter 压测数据集）。
 *
 * 手动执行（需要重建 k6 夹具时）：
 *   cd campus-trade-api
 *   mvnw.cmd test -Dtest=PerformanceFixtureSetup
 *
 * 【清理范围】仅删除 perf-buyer(990001) / perf-seller(990002) 两个夹具账号自身的
 * 用户、商品、订单与资金数据，不触碰库内其他任何数据。
 */
@ActiveProfiles("test")
@SpringBootTest(properties = {"orders.payment-expiration-enabled=false", "security.bootstrap-admin.enabled=false", "ai.enabled=false", "spring.mail.host=localhost"})
class PerformanceFixtureSetup {
    private static final String BUYER_ID = "990001";
    private static final String SELLER_ID = "990002";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private FinanceService financeService;

    //夹具重建：先清掉 perf 账号的旧数据（按外键依赖顺序），再建新账号并充值
    @Test
    void preparesIsolatedPerformanceUsersAndBalance() {
        jdbc.update("DELETE FROM notifications WHERE user_id IN (?, ?)", BUYER_ID, SELLER_ID);
        //资金单据（引用 orders，必须先删）
        jdbc.update("DELETE FROM account_flow WHERE user_id IN (?, ?)", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM settlement_order WHERE buyer_id = ? OR seller_id = ?", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM refund_order WHERE buyer_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM account_freeze_record WHERE user_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM payment_order WHERE buyer_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM recharge_order WHERE user_id = ?", BUYER_ID);
        jdbc.update("DELETE FROM account WHERE user_id IN (?, ?)", BUYER_ID, SELLER_ID);
        //业务数据（只删 perf 账号自己的）
        jdbc.update("DELETE FROM ratings WHERE order_id IN (SELECT id FROM (SELECT id FROM orders WHERE buyer_id = ? OR seller_id = ?) t)", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM orders WHERE buyer_id = ? OR seller_id = ?", BUYER_ID, SELLER_ID);
        jdbc.update("DELETE FROM product WHERE seller_id = ?", SELLER_ID);
        jdbc.update("DELETE FROM user WHERE id IN (?, ?)", BUYER_ID, SELLER_ID);

        String password = new BCryptPasswordEncoder().encode("PerfPass_2026!");
        jdbc.update("INSERT INTO user(id, username, password, nickname, role, credit_score, status) VALUES (990001, 'perf-buyer', ?, 'Performance Buyer', 'USER', 100, 1)", password);
        jdbc.update("INSERT INTO user(id, username, password, nickname, role, credit_score, status) VALUES (990002, 'perf-seller', ?, 'Performance Seller', 'USER', 100, 1)", password);
        RechargeRequest recharge = new RechargeRequest(); recharge.setAmount(new BigDecimal("100000.00")); recharge.setRequestId("perf-initial-recharge");
        financeService.recharge("990001", recharge);
    }
}
