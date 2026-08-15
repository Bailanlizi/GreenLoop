package com.campus.trade.mapper;

import com.campus.trade.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FinanceMapper {
    int insertAccountIfAbsent(@Param("userId") String userId);
    Account findAccountByUserId(String userId);
    Account findAccountByUserIdForUpdate(String userId);
    int creditAvailable(@Param("accountId") String accountId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
    int freezeBalance(@Param("accountId") String accountId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
    int refundFrozen(@Param("accountId") String accountId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
    int debitFrozen(@Param("accountId") String accountId, @Param("amount") BigDecimal amount, @Param("version") Integer version);

    void insertAccountFlow(AccountFlow flow);
    List<AccountFlow> findFlowsByUserId(@Param("userId") String userId, @Param("businessType") String businessType);

    void insertRechargeOrder(RechargeOrder rechargeOrder);
    RechargeOrder findRechargeByRequest(@Param("userId") String userId, @Param("requestId") String requestId);
    List<RechargeOrder> findRechargesByUserId(String userId);

    void insertPaymentOrder(PaymentOrder paymentOrder);
    PaymentOrder findPaymentByRequest(@Param("buyerId") String buyerId, @Param("requestId") String requestId);
    PaymentOrder findPaymentByOrderId(String orderId);
    int updatePaymentStatus(@Param("orderId") String orderId, @Param("currentStatus") String currentStatus, @Param("targetStatus") String targetStatus);

    void insertFreezeRecord(AccountFreezeRecord record);
    int updateFreezeStatus(@Param("orderId") String orderId, @Param("currentStatus") String currentStatus, @Param("targetStatus") String targetStatus);

    void insertRefundOrder(RefundOrder refundOrder);
    RefundOrder findRefundByOrderId(String orderId);
    void insertSettlementOrder(SettlementOrder settlementOrder);
    SettlementOrder findSettlementByOrderId(String orderId);

    List<Account> findAccountsForAdmin(@Param("keyword") String keyword, @Param("status") String status);
    List<PaymentOrder> findPaymentsForAdmin(@Param("orderId") String orderId, @Param("status") String status);
    List<RefundOrder> findRefundsForAdmin(@Param("orderId") String orderId, @Param("status") String status);
    List<SettlementOrder> findSettlementsForAdmin(@Param("orderId") String orderId, @Param("status") String status);
    List<AccountFlow> findFlowsForAdmin(@Param("keyword") String keyword, @Param("businessType") String businessType);
}
