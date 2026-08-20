package com.campus.trade.service.impl;

import com.campus.trade.domain.OrderStatus;
import com.campus.trade.dto.PageResult;
import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.entity.*;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.FinanceMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.service.FinanceService;
import com.campus.trade.service.NotificationEventService;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class FinanceServiceImpl implements FinanceService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final FinanceMapper financeMapper;
    private final OrderMapper orderMapper;
    private final NotificationEventService notificationEvents;

    public FinanceServiceImpl(FinanceMapper financeMapper, OrderMapper orderMapper,
                              NotificationEventService notificationEvents) {
        this.financeMapper = financeMapper;
        this.orderMapper = orderMapper;
        this.notificationEvents = notificationEvents;
    }

    @Override
    @Transactional
    public void ensureAccount(String userId) {
        financeMapper.insertAccountIfAbsent(userId);
    }

    @Override
    @Transactional
    public Account getAccount(String userId) {
        ensureAccount(userId);
        return financeMapper.findAccountByUserId(userId);
    }

    @Override
    @Transactional
    public RechargeOrder recharge(String userId, RechargeRequest request) {
        validateRequestId(request.getRequestId());
        RechargeOrder existing = financeMapper.findRechargeByRequest(userId, request.getRequestId());
        if (existing != null) return existing;

        ensureAccount(userId);
        Account account = requireActiveAccountForUpdate(userId);
        existing = financeMapper.findRechargeByRequest(userId, request.getRequestId());
        if (existing != null) return existing;

        BigDecimal amount = normalizeAmount(request.getAmount());
        String rechargeNo = businessNo("RC");
        if (financeMapper.creditAvailable(account.getId(), amount, account.getVersion()) != 1) {
            throw new CustomException("账户状态已变化，请重试");
        }

        RechargeOrder recharge = new RechargeOrder();
        recharge.setRechargeNo(rechargeNo);
        recharge.setUserId(userId);
        recharge.setRequestId(request.getRequestId());
        recharge.setAmount(amount);
        recharge.setStatus("SUCCESS");
        financeMapper.insertRechargeOrder(recharge);
        insertFlow(account, "RECHARGE", rechargeNo, amount, ZERO, "模拟充值");
        return financeMapper.findRechargeByRequest(userId, request.getRequestId());
    }

    @Override
    public PageResult<AccountFlow> getFlows(String userId, String businessType, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findFlowsByUserId(userId, businessType));
    }

    @Override
    public PageResult<RechargeOrder> getRecharges(String userId, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findRechargesByUserId(userId));
    }

    @Override
    @Transactional
    public PaymentOrder payOrder(String orderId, String buyerId, PaymentRequest request) {
        validateRequestId(request.getRequestId());
        PaymentOrder requestPayment = financeMapper.findPaymentByRequest(buyerId, request.getRequestId());
        if (requestPayment != null) {
            if (!Objects.equals(orderId, requestPayment.getOrderId())) {
                throw new CustomException("请求号已用于其他订单");
            }
            return requestPayment;
        }

        Order order = requireOrderForUpdate(orderId);
        if (!Objects.equals(order.getBuyerId(), buyerId)) throw new CustomException("无权支付此订单");

        PaymentOrder existingPayment = financeMapper.findPaymentByOrderId(orderId);
        if (existingPayment != null) return existingPayment;
        if (!OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())) {
            throw new CustomException("当前订单状态不允许支付");
        }
        if (order.getPaymentDeadline() == null || !order.getPaymentDeadline().after(new Date())) {
            throw new CustomException("订单已超过支付期限");
        }

        ensureAccount(buyerId);
        Account account = requireActiveAccountForUpdate(buyerId);
        requestPayment = financeMapper.findPaymentByRequest(buyerId, request.getRequestId());
        if (requestPayment != null) {
            if (!Objects.equals(orderId, requestPayment.getOrderId())) {
                throw new CustomException("请求号已用于其他订单");
            }
            return requestPayment;
        }

        BigDecimal amount = normalizeAmount(order.getTotalPrice());
        if (financeMapper.freezeBalance(account.getId(), amount, account.getVersion()) != 1) {
            throw new CustomException("可用余额不足或账户状态已变化");
        }

        String paymentNo = businessNo("PY");
        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(orderId);
        payment.setBuyerId(buyerId);
        payment.setRequestId(request.getRequestId());
        payment.setAmount(amount);
        payment.setStatus("FROZEN");
        financeMapper.insertPaymentOrder(payment);

        AccountFreezeRecord freeze = new AccountFreezeRecord();
        freeze.setFreezeNo(businessNo("FZ"));
        freeze.setOrderId(orderId);
        freeze.setPaymentNo(paymentNo);
        freeze.setAccountId(account.getId());
        freeze.setUserId(buyerId);
        freeze.setAmount(amount);
        freeze.setStatus("FROZEN");
        financeMapper.insertFreezeRecord(freeze);
        insertFlow(account, "PAYMENT_FREEZE", paymentNo, amount.negate(), amount, "订单支付冻结");

        String targetStatus = "MEETUP".equals(order.getDeliveryMethod())
                ? OrderStatus.AWAITING_MEETUP.name() : OrderStatus.AWAITING_SHIPMENT.name();
        if (orderMapper.updateOrderStatusIfCurrent(orderId, OrderStatus.PENDING_PAYMENT.name(), targetStatus) != 1) {
            throw new CustomException("订单状态已变化，请刷新后重试");
        }
        notificationEvents.order(order.getSellerId(), "ORDER_PAID", "订单已支付，等待您履约。", orderId);
        return financeMapper.findPaymentByOrderId(orderId);
    }

    @Override
    public PaymentOrder getPayment(String orderId, String userId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) throw new CustomException("订单不存在");
        if (!Objects.equals(order.getBuyerId(), userId) && !Objects.equals(order.getSellerId(), userId)) {
            throw new CustomException("无权查看此支付记录");
        }
        return financeMapper.findPaymentByOrderId(orderId);
    }

    @Override
    @Transactional
    public void refundPaidOrder(Order order) {
        if (financeMapper.findRefundByOrderId(order.getId()) != null) return;
        PaymentOrder payment = financeMapper.findPaymentByOrderId(order.getId());
        if (payment == null && order.getPaymentDeadline() == null) return;
        requireFrozenPayment(payment);
        Account buyerAccount = requireActiveAccountForUpdate(order.getBuyerId());
        BigDecimal amount = payment.getAmount();
        if (financeMapper.refundFrozen(buyerAccount.getId(), amount, buyerAccount.getVersion()) != 1) {
            throw new CustomException("买家冻结余额异常，无法退款");
        }
        if (financeMapper.updatePaymentStatus(order.getId(), "FROZEN", "REFUNDED") != 1
                || financeMapper.updateFreezeStatus(order.getId(), "FROZEN", "RELEASED") != 1) {
            throw new CustomException("支付冻结状态异常，无法退款");
        }
        String refundNo = businessNo("RF");
        RefundOrder refund = new RefundOrder();
        refund.setRefundNo(refundNo);
        refund.setOrderId(order.getId());
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setBuyerId(order.getBuyerId());
        refund.setAmount(amount);
        refund.setStatus("SUCCESS");
        financeMapper.insertRefundOrder(refund);
        insertFlow(buyerAccount, "REFUND", refundNo, amount, amount.negate(), "订单取消退款");
    }

    @Override
    @Transactional
    public void settleOrder(Order order) {
        if (financeMapper.findSettlementByOrderId(order.getId()) != null) return;
        PaymentOrder payment = financeMapper.findPaymentByOrderId(order.getId());
        if (payment == null && order.getPaymentDeadline() == null) return;
        requireFrozenPayment(payment);
        ensureAccount(order.getBuyerId());
        ensureAccount(order.getSellerId());

        Account first;
        Account second;
        if (compareIds(order.getBuyerId(), order.getSellerId()) <= 0) {
            first = requireActiveAccountForUpdate(order.getBuyerId());
            second = requireActiveAccountForUpdate(order.getSellerId());
        } else {
            first = requireActiveAccountForUpdate(order.getSellerId());
            second = requireActiveAccountForUpdate(order.getBuyerId());
        }
        Account buyer = Objects.equals(first.getUserId(), order.getBuyerId()) ? first : second;
        Account seller = Objects.equals(first.getUserId(), order.getSellerId()) ? first : second;
        BigDecimal amount = payment.getAmount();

        if (financeMapper.debitFrozen(buyer.getId(), amount, buyer.getVersion()) != 1) {
            throw new CustomException("买家冻结余额异常，无法结算");
        }
        if (financeMapper.creditAvailable(seller.getId(), amount, seller.getVersion()) != 1) {
            throw new CustomException("卖家账户状态异常，无法结算");
        }
        if (financeMapper.updatePaymentStatus(order.getId(), "FROZEN", "SETTLED") != 1
                || financeMapper.updateFreezeStatus(order.getId(), "FROZEN", "SETTLED") != 1) {
            throw new CustomException("支付冻结状态异常，无法结算");
        }

        String settlementNo = businessNo("ST");
        SettlementOrder settlement = new SettlementOrder();
        settlement.setSettlementNo(settlementNo);
        settlement.setOrderId(order.getId());
        settlement.setPaymentNo(payment.getPaymentNo());
        settlement.setBuyerId(order.getBuyerId());
        settlement.setSellerId(order.getSellerId());
        settlement.setAmount(amount);
        settlement.setStatus("SUCCESS");
        financeMapper.insertSettlementOrder(settlement);
        insertFlow(buyer, "SETTLEMENT_OUT", settlementNo, ZERO, amount.negate(), "确认收货结算");
        insertFlow(seller, "SETTLEMENT_IN", settlementNo, amount, ZERO, "订单销售入账");
    }

    @Override
    public PageResult<Account> findAccounts(String keyword, String status, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findAccountsForAdmin(keyword, status));
    }

    @Override
    public PageResult<PaymentOrder> findPayments(String orderId, String status, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findPaymentsForAdmin(orderId, status));
    }

    @Override
    public PageResult<RefundOrder> findRefunds(String orderId, String status, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findRefundsForAdmin(orderId, status));
    }

    @Override
    public PageResult<SettlementOrder> findSettlements(String orderId, String status, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findSettlementsForAdmin(orderId, status));
    }

    @Override
    public PageResult<AccountFlow> findFlows(String keyword, String businessType, Integer page, Integer size) {
        PageHelper.startPage(normalizePage(page), normalizeSize(size));
        return new PageResult<>(financeMapper.findFlowsForAdmin(keyword, businessType));
    }

    private Account requireActiveAccountForUpdate(String userId) {
        Account account = financeMapper.findAccountByUserIdForUpdate(userId);
        if (account == null) throw new CustomException("资金账户不存在");
        if (!"ACTIVE".equals(account.getStatus())) throw new CustomException("资金账户不可用");
        return account;
    }

    private Order requireOrderForUpdate(String orderId) {
        Order order = orderMapper.findOrderByIdForUpdate(orderId);
        if (order == null) throw new CustomException("订单不存在");
        return order;
    }

    private void requireFrozenPayment(PaymentOrder payment) {
        if (payment == null || !"FROZEN".equals(payment.getStatus())) {
            throw new CustomException("订单支付记录异常");
        }
    }

    private void insertFlow(Account before, String type, String businessNo,
                            BigDecimal availableChange, BigDecimal frozenChange, String remark) {
        AccountFlow flow = new AccountFlow();
        flow.setFlowNo(businessNo("FL"));
        flow.setAccountId(before.getId());
        flow.setUserId(before.getUserId());
        flow.setBusinessType(type);
        flow.setBusinessNo(businessNo);
        flow.setAvailableChange(availableChange);
        flow.setFrozenChange(frozenChange);
        flow.setAvailableBefore(before.getAvailableBalance());
        flow.setAvailableAfter(before.getAvailableBalance().add(availableChange));
        flow.setFrozenBefore(before.getFrozenBalance());
        flow.setFrozenAfter(before.getFrozenBalance().add(frozenChange));
        flow.setRemark(remark);
        financeMapper.insertAccountFlow(flow);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.scale() > 2 || amount.compareTo(ZERO) <= 0) {
            throw new CustomException("金额必须大于0且最多保留两位小数");
        }
        return amount.setScale(2);
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64) {
            throw new CustomException("请求号不能为空且长度不能超过64位");
        }
    }

    private String businessNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private int compareIds(String left, String right) {
        try {
            return Long.compare(Long.parseLong(left), Long.parseLong(right));
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }

    private int normalizePage(Integer page) { return page == null || page < 1 ? 1 : page; }
    private int normalizeSize(Integer size) { return size == null ? 10 : Math.max(1, Math.min(size, 100)); }
}
