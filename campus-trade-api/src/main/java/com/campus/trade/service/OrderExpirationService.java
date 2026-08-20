package com.campus.trade.service;

import com.campus.trade.domain.OrderStatus;
import com.campus.trade.domain.ProductStatus;
import com.campus.trade.entity.Order;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.NotificationEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderExpirationService {
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final NotificationEventService notificationEvents;

    public OrderExpirationService(OrderMapper orderMapper, ProductMapper productMapper, NotificationEventService notificationEvents) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.notificationEvents = notificationEvents;
    }

    @Transactional
    public boolean expirePendingOrder(String orderId) {
        Order order = orderMapper.findOrderByIdForUpdate(orderId);
        if (order == null || !OrderStatus.PENDING_PAYMENT.name().equals(order.getOrderStatus())
                || order.getPaymentDeadline() == null
                || order.getPaymentDeadline().getTime() > System.currentTimeMillis()) {
            return false;
        }
        if (orderMapper.updateOrderStatusIfCurrent(orderId, OrderStatus.PENDING_PAYMENT.name(), OrderStatus.CANCELLED.name()) != 1) {
            return false;
        }
        if (productMapper.updateProductStatusIfCurrent(order.getProductId(), ProductStatus.LOCKED.name(), ProductStatus.AVAILABLE.name()) != 1) {
            throw new CustomException("超时订单商品状态异常");
        }
        notificationEvents.order(order.getBuyerId(), "ORDER_PAYMENT_EXPIRED", "订单支付超时，已自动取消。", order.getId());
        notificationEvents.order(order.getSellerId(), "ORDER_PAYMENT_EXPIRED", "订单支付超时，商品已恢复可售。", order.getId());
        return true;
    }
}
