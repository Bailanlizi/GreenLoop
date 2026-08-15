package com.campus.trade.service;

import com.campus.trade.domain.OrderStatus;
import com.campus.trade.domain.ProductStatus;
import com.campus.trade.entity.Order;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderExpirationService {
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;

    public OrderExpirationService(OrderMapper orderMapper, ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
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
        return true;
    }
}
