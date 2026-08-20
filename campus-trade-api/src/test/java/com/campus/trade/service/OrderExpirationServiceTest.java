package com.campus.trade.service;

import com.campus.trade.entity.Order;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.NotificationEventService;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class OrderExpirationServiceTest {
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final OrderExpirationService service = new OrderExpirationService(orderMapper, productMapper, mock(NotificationEventService.class));

    @Test
    void expiredPendingOrderIsCancelledAndProductReleased() {
        Order order = order(new Date(System.currentTimeMillis() - 1000));
        when(orderMapper.findOrderByIdForUpdate("order-1")).thenReturn(order);
        when(orderMapper.updateOrderStatusIfCurrent("order-1", "PENDING_PAYMENT", "CANCELLED")).thenReturn(1);
        when(productMapper.updateProductStatusIfCurrent("product-1", "LOCKED", "AVAILABLE")).thenReturn(1);

        assertTrue(service.expirePendingOrder("order-1"));
        verify(productMapper).updateProductStatusIfCurrent("product-1", "LOCKED", "AVAILABLE");
    }

    @Test
    void unexpiredOrderIsNotChanged() {
        when(orderMapper.findOrderByIdForUpdate("order-1")).thenReturn(order(new Date(System.currentTimeMillis() + 60000)));

        assertFalse(service.expirePendingOrder("order-1"));
        verify(orderMapper, never()).updateOrderStatusIfCurrent(any(), any(), any());
    }

    private Order order(Date deadline) {
        Order order = new Order();
        order.setId("order-1");
        order.setProductId("product-1");
        order.setOrderStatus("PENDING_PAYMENT");
        order.setPaymentDeadline(deadline);
        return order;
    }
}
