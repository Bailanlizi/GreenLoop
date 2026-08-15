package com.campus.trade.service;

import com.campus.trade.dto.PageResult;
import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.entity.*;

public interface FinanceService {
    void ensureAccount(String userId);
    Account getAccount(String userId);
    RechargeOrder recharge(String userId, RechargeRequest request);
    PageResult<AccountFlow> getFlows(String userId, String businessType, Integer page, Integer size);
    PageResult<RechargeOrder> getRecharges(String userId, Integer page, Integer size);
    PaymentOrder payOrder(String orderId, String buyerId, PaymentRequest request);
    PaymentOrder getPayment(String orderId, String userId);
    void refundPaidOrder(Order order);
    void settleOrder(Order order);

    PageResult<Account> findAccounts(String keyword, String status, Integer page, Integer size);
    PageResult<PaymentOrder> findPayments(String orderId, String status, Integer page, Integer size);
    PageResult<RefundOrder> findRefunds(String orderId, String status, Integer page, Integer size);
    PageResult<SettlementOrder> findSettlements(String orderId, String status, Integer page, Integer size);
    PageResult<AccountFlow> findFlows(String keyword, String businessType, Integer page, Integer size);
}
