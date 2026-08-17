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

@ActiveProfiles("test")
@SpringBootTest(properties = {"orders.payment-expiration-enabled=false", "security.bootstrap-admin.enabled=false", "ai.enabled=false", "spring.mail.host=localhost"})
class PerformanceFixtureSetupTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FinanceService financeService;

    @Test
    void preparesIsolatedPerformanceUsersAndBalance() {
        jdbc.update("DELETE FROM account_flow"); jdbc.update("DELETE FROM settlement_order"); jdbc.update("DELETE FROM refund_order");
        jdbc.update("DELETE FROM account_freeze_record"); jdbc.update("DELETE FROM payment_order"); jdbc.update("DELETE FROM recharge_order");
        jdbc.update("DELETE FROM account"); jdbc.update("DELETE FROM ratings"); jdbc.update("DELETE FROM orders"); jdbc.update("DELETE FROM product"); jdbc.update("DELETE FROM user");
        String password = new BCryptPasswordEncoder().encode("PerfPass_2026!");
        jdbc.update("INSERT INTO user(id, username, password, nickname, role, credit_score, status) VALUES (990001, 'perf-buyer', ?, 'Performance Buyer', 'USER', 100, 1)", password);
        jdbc.update("INSERT INTO user(id, username, password, nickname, role, credit_score, status) VALUES (990002, 'perf-seller', ?, 'Performance Seller', 'USER', 100, 1)", password);
        RechargeRequest recharge = new RechargeRequest(); recharge.setAmount(new BigDecimal("100000.00")); recharge.setRequestId("perf-initial-recharge");
        financeService.recharge("990001", recharge);
    }
}
