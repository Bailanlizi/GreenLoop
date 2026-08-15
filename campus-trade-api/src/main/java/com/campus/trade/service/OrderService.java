package com.campus.trade.service;

import com.campus.trade.dto.CreateOrderDTO;
import com.campus.trade.dto.DeliveryStatsDTO;
import com.campus.trade.dto.ShipmentDTO;
import com.campus.trade.dto.PageResult; // 【新增】
import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.entity.PaymentOrder;
import com.campus.trade.entity.Order;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface OrderService {
    Order createOrder(String buyerId, CreateOrderDTO createOrderDTO);
    PaymentOrder payOrder(String orderId, String buyerId, PaymentRequest request);
    PaymentOrder getPayment(String orderId, String userId);
    Order getOrderDetails(String orderId);
    List<Order> getMyPurchases(String userId);
    List<Order> getMySales(String userId);
    Order cancelOrder(String orderId, String buyerId);
    Order confirmCompletion(String orderId, String buyerId);
    Order shipOrder(String orderId, String sellerId, ShipmentDTO shipmentDTO);
    Order forceCancelOrder(String orderId);

    // 【修改】为管理员查询所有订单的方法，增加分页参数
    PageResult<Order> findAllOrdersForAdmin(String orderId, String deliveryMethod, Integer page, Integer size);

    // 配送管理相关方法
    DeliveryStatsDTO getDeliveryStats();
    void exportDeliveryOrders(String orderId, String deliveryMethod, String orderStatus, HttpServletResponse response) throws IOException;
}
