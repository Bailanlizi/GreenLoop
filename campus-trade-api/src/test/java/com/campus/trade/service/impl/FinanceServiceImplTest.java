package com.campus.trade.service.impl;

import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.entity.Account;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.PaymentOrder;
import com.campus.trade.entity.RechargeOrder;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.FinanceMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FinanceServiceImplTest {
    private FinanceMapper financeMapper;
    private OrderMapper orderMapper;
    private FinanceServiceImpl financeService;

    @BeforeEach
    void setUp() {
        financeMapper = mock(FinanceMapper.class);
        orderMapper = mock(OrderMapper.class);
        financeService = new FinanceServiceImpl(financeMapper, orderMapper, mock(NotificationService.class));
    }

    @Test
    void repeatedRechargeReturnsExistingBusinessWithoutCreditingAgain() {
        RechargeOrder existing = new RechargeOrder();
        existing.setRechargeNo("RC1");
        when(financeMapper.findRechargeByRequest("buyer-1", "request-1")).thenReturn(existing);

        RechargeRequest request = new RechargeRequest();
        request.setRequestId("request-1");
        request.setAmount(new BigDecimal("100.00"));

        assertEquals("RC1", financeService.recharge("buyer-1", request).getRechargeNo());
        verify(financeMapper, never()).creditAvailable(any(), any(), any());
    }

    @Test
    void paymentFreezesBalanceAndAdvancesOrderToFulfillment() {
        Order order = pendingOrder();
        Account account = account("account-1", "buyer-1", "200.00", "0.00", 3);
        PaymentOrder saved = new PaymentOrder();
        saved.setOrderId("order-1");
        saved.setStatus("FROZEN");
        when(orderMapper.findOrderByIdForUpdate("order-1")).thenReturn(order);
        when(financeMapper.findAccountByUserIdForUpdate("buyer-1")).thenReturn(account);
        when(financeMapper.freezeBalance("account-1", new BigDecimal("100.00"), 3)).thenReturn(1);
        when(orderMapper.updateOrderStatusIfCurrent("order-1", "PENDING_PAYMENT", "AWAITING_SHIPMENT")).thenReturn(1);
        when(financeMapper.findPaymentByOrderId("order-1")).thenReturn(null, saved);

        PaymentRequest request = new PaymentRequest();
        request.setRequestId("pay-request-1");
        PaymentOrder result = financeService.payOrder("order-1", "buyer-1", request);

        assertEquals("FROZEN", result.getStatus());
        verify(financeMapper).insertPaymentOrder(any(PaymentOrder.class));
        verify(financeMapper).insertFreezeRecord(any());
        verify(financeMapper).insertAccountFlow(any());
    }

    @Test
    void insufficientBalanceDoesNotCreatePayment() {
        Order order = pendingOrder();
        Account account = account("account-1", "buyer-1", "20.00", "0.00", 1);
        when(orderMapper.findOrderByIdForUpdate("order-1")).thenReturn(order);
        when(financeMapper.findAccountByUserIdForUpdate("buyer-1")).thenReturn(account);
        when(financeMapper.freezeBalance(any(), any(), any())).thenReturn(0);

        PaymentRequest request = new PaymentRequest();
        request.setRequestId("pay-request-2");

        assertThrows(CustomException.class, () -> financeService.payOrder("order-1", "buyer-1", request));
        verify(financeMapper, never()).insertPaymentOrder(any());
    }

    @Test
    void rechargeCreatesAppendOnlyFlowWithBalanceSnapshots() {
        Account account = account("account-1", "buyer-1", "25.00", "5.00", 2);
        RechargeOrder saved = new RechargeOrder();
        saved.setStatus("SUCCESS");
        when(financeMapper.findAccountByUserIdForUpdate("buyer-1")).thenReturn(account);
        when(financeMapper.creditAvailable("account-1", new BigDecimal("10.00"), 2)).thenReturn(1);
        when(financeMapper.findRechargeByRequest("buyer-1", "request-3")).thenReturn(null, null, saved);

        RechargeRequest request = new RechargeRequest();
        request.setRequestId("request-3");
        request.setAmount(new BigDecimal("10.00"));
        financeService.recharge("buyer-1", request);

        ArgumentCaptor<com.campus.trade.entity.AccountFlow> captor = ArgumentCaptor.forClass(com.campus.trade.entity.AccountFlow.class);
        verify(financeMapper).insertAccountFlow(captor.capture());
        assertEquals(new BigDecimal("35.00"), captor.getValue().getAvailableAfter());
        assertEquals(new BigDecimal("5.00"), captor.getValue().getFrozenAfter());
    }

    @Test
    void requestIdCannotBeReusedForAnotherOrderAfterAccountLock() {
        Order order = pendingOrder();
        Account account = account("account-1", "buyer-1", "200.00", "0.00", 1);
        PaymentOrder otherPayment = new PaymentOrder();
        otherPayment.setOrderId("order-other");
        when(orderMapper.findOrderByIdForUpdate("order-1")).thenReturn(order);
        when(financeMapper.findAccountByUserIdForUpdate("buyer-1")).thenReturn(account);
        when(financeMapper.findPaymentByRequest("buyer-1", "same-request")).thenReturn(null, otherPayment);

        PaymentRequest request = new PaymentRequest();
        request.setRequestId("same-request");

        assertThrows(CustomException.class, () -> financeService.payOrder("order-1", "buyer-1", request));
        verify(financeMapper, never()).freezeBalance(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 50, 100})
    void repeatedPaymentWithSameRequestIdRecordsFundsOnce(int replayCount) {
        PaymentOrder existing = payment("PY1", "FROZEN");
        existing.setRequestId("same-payment-request");
        when(financeMapper.findPaymentByRequest("buyer-1", "same-payment-request"))
                .thenReturn(existing);

        PaymentRequest request = new PaymentRequest();
        request.setRequestId("same-payment-request");
        for (int i = 0; i < replayCount; i++) {
            assertEquals("PY1", financeService.payOrder("order-1", "buyer-1", request).getPaymentNo());
        }

        verify(financeMapper, never()).freezeBalance(any(), any(), any());
        verify(financeMapper, never()).insertPaymentOrder(any());
        verify(financeMapper, never()).insertFreezeRecord(any());
        verify(financeMapper, never()).insertAccountFlow(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 50, 100})
    void repeatedRechargeWithSameRequestIdRecordsFundsOnce(int replayCount) {
        RechargeOrder existing = new RechargeOrder();
        existing.setRechargeNo("RC1");
        existing.setRequestId("same-recharge-request");
        when(financeMapper.findRechargeByRequest("buyer-1", "same-recharge-request"))
                .thenReturn(existing);

        RechargeRequest request = new RechargeRequest();
        request.setRequestId("same-recharge-request");
        request.setAmount(new BigDecimal("10.00"));
        for (int i = 0; i < replayCount; i++) {
            assertEquals("RC1", financeService.recharge("buyer-1", request).getRechargeNo());
        }

        verify(financeMapper, never()).creditAvailable(any(), any(), any());
        verify(financeMapper, never()).insertRechargeOrder(any());
        verify(financeMapper, never()).insertAccountFlow(any());
    }

    @Test
    void paidOrderCancellationReleasesFrozenBalanceAndWritesRefund() {
        Order order = pendingOrder();
        PaymentOrder payment = payment("PY1", "FROZEN");
        Account buyer = account("account-1", "buyer-1", "50.00", "100.00", 4);
        when(financeMapper.findPaymentByOrderId("order-1")).thenReturn(payment);
        when(financeMapper.findAccountByUserIdForUpdate("buyer-1")).thenReturn(buyer);
        when(financeMapper.refundFrozen("account-1", new BigDecimal("100.00"), 4)).thenReturn(1);
        when(financeMapper.updatePaymentStatus("order-1", "FROZEN", "REFUNDED")).thenReturn(1);
        when(financeMapper.updateFreezeStatus("order-1", "FROZEN", "RELEASED")).thenReturn(1);

        financeService.refundPaidOrder(order);

        verify(financeMapper).insertRefundOrder(any());
        verify(financeMapper).insertAccountFlow(any());
    }

    @Test
    void completedOrderTransfersFrozenBalanceToSeller() {
        Order order = pendingOrder();
        PaymentOrder payment = payment("PY1", "FROZEN");
        Account buyer = account("buyer-account", "buyer-1", "20.00", "100.00", 2);
        Account seller = account("seller-account", "seller-1", "30.00", "0.00", 5);
        when(financeMapper.findPaymentByOrderId("order-1")).thenReturn(payment);
        when(financeMapper.findAccountByUserIdForUpdate("buyer-1")).thenReturn(buyer);
        when(financeMapper.findAccountByUserIdForUpdate("seller-1")).thenReturn(seller);
        when(financeMapper.debitFrozen("buyer-account", new BigDecimal("100.00"), 2)).thenReturn(1);
        when(financeMapper.creditAvailable("seller-account", new BigDecimal("100.00"), 5)).thenReturn(1);
        when(financeMapper.updatePaymentStatus("order-1", "FROZEN", "SETTLED")).thenReturn(1);
        when(financeMapper.updateFreezeStatus("order-1", "FROZEN", "SETTLED")).thenReturn(1);

        financeService.settleOrder(order);

        verify(financeMapper).insertSettlementOrder(any());
        verify(financeMapper, times(2)).insertAccountFlow(any());
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId("order-1");
        order.setBuyerId("buyer-1");
        order.setSellerId("seller-1");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setDeliveryMethod("SHIPPING");
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setPaymentDeadline(new Date(System.currentTimeMillis() + 60000));
        return order;
    }

    private Account account(String id, String userId, String available, String frozen, int version) {
        Account account = new Account();
        account.setId(id);
        account.setUserId(userId);
        account.setAvailableBalance(new BigDecimal(available));
        account.setFrozenBalance(new BigDecimal(frozen));
        account.setVersion(version);
        account.setStatus("ACTIVE");
        return account;
    }

    private PaymentOrder payment(String paymentNo, String status) {
        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId("order-1");
        payment.setBuyerId("buyer-1");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(status);
        return payment;
    }
}
