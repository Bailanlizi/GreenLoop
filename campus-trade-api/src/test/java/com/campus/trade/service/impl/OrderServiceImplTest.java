package com.campus.trade.service.impl;

import com.campus.trade.dto.CreateOrderDTO;
import com.campus.trade.dto.ShipmentDTO;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private OrderMapper orderMapper;
    private ProductMapper productMapper;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        productMapper = mock(ProductMapper.class);
        orderService = new OrderServiceImpl(orderMapper, productMapper, mock(NotificationService.class));
    }

    @Test
    void createOrderLocksAvailableProductBeforeCreatingOrder() {
        Product product = product("product-1", "seller-1");
        CreateOrderDTO request = shippingRequest("product-1");
        when(productMapper.lockProductIfAvailable("product-1")).thenReturn(1);
        when(productMapper.findProductById("product-1")).thenReturn(product);
        when(orderMapper.findOrderById(any())).thenAnswer(invocation -> {
            Order order = new Order();
            order.setId(invocation.getArgument(0));
            return order;
        });

        orderService.createOrder("buyer-1", request);

        verify(productMapper).lockProductIfAvailable("product-1");
        verify(orderMapper).insertOrder(any(Order.class));
    }

    @Test
    void createOrderDoesNotCreateOrderWhenProductIsAlreadyLocked() {
        when(productMapper.lockProductIfAvailable("product-1")).thenReturn(0);

        assertThrows(CustomException.class, () -> orderService.createOrder("buyer-1", shippingRequest("product-1")));

        verify(orderMapper, never()).insertOrder(any());
    }

    @Test
    void buyerCanCancelAwaitingShipmentAndReleaseProduct() {
        Order order = order("order-1", "buyer-1", "seller-1", "product-1", "AWAITING_SHIPMENT", "SHIPPING");
        when(orderMapper.findOrderById("order-1")).thenReturn(order);
        when(orderMapper.updateOrderStatusIfCurrent("order-1", "AWAITING_SHIPMENT", "CANCELLED")).thenReturn(1);
        when(productMapper.updateProductStatusIfCurrent("product-1", "LOCKED", "AVAILABLE")).thenReturn(1);

        orderService.cancelOrder("order-1", "buyer-1");

        verify(productMapper).updateProductStatusIfCurrent("product-1", "LOCKED", "AVAILABLE");
    }

    @Test
    void buyerCanOnlyConfirmShippedDeliveryOrder() {
        Order order = order("order-1", "buyer-1", "seller-1", "product-1", "SHIPPED", "SHIPPING");
        when(orderMapper.findOrderById("order-1")).thenReturn(order);
        when(orderMapper.updateOrderStatusIfCurrent("order-1", "SHIPPED", "COMPLETED")).thenReturn(1);
        when(productMapper.updateProductStatusIfCurrent("product-1", "LOCKED", "SOLD")).thenReturn(1);

        orderService.confirmCompletion("order-1", "buyer-1");

        verify(productMapper).updateProductStatusIfCurrent("product-1", "LOCKED", "SOLD");
    }

    @Test
    void onlySellerCanShipOrder() {
        Order order = order("order-1", "buyer-1", "seller-1", "product-1", "AWAITING_SHIPMENT", "SHIPPING");
        when(orderMapper.findOrderById("order-1")).thenReturn(order);

        assertThrows(CustomException.class, () -> orderService.shipOrder("order-1", "buyer-1", new ShipmentDTO("顺丰", "SF123")));

        verify(orderMapper, never()).markOrderShipped(any(), any(), any());
    }

    private CreateOrderDTO shippingRequest(String productId) {
        CreateOrderDTO request = new CreateOrderDTO();
        request.setProductId(productId);
        request.setDeliveryMethod("SHIPPING");
        request.setShippingAddressId(1L);
        return request;
    }

    private Product product(String productId, String sellerId) {
        Product product = new Product();
        product.setId(productId);
        product.setSellerId(sellerId);
        product.setPrice(new BigDecimal("100.00"));
        product.setTitle("测试商品");
        product.setDeliveryOptions("SHIPPING,MEETUP");
        return product;
    }

    private Order order(String id, String buyerId, String sellerId, String productId, String status, String deliveryMethod) {
        Order order = new Order();
        order.setId(id);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setProductId(productId);
        order.setOrderStatus(status);
        order.setDeliveryMethod(deliveryMethod);
        return order;
    }
}
