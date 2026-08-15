package com.campus.trade.mapper;

import com.campus.trade.dto.DailyStatsDTO;
import com.campus.trade.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderMapper {
    Order findOrderById(String orderId);
    Order findOrderByIdForUpdate(String orderId);
    List<Order> findOrdersByBuyerId(String buyerId);
    List<Order> findOrdersBySellerId(String sellerId);
    void insertOrder(Order order);
    int updateOrderStatusIfCurrent(@Param("orderId") String orderId,
                                   @Param("currentStatus") String currentStatus,
                                   @Param("targetStatus") String targetStatus);
    int markOrderShipped(@Param("orderId") String orderId,
                         @Param("shippingProvider") String shippingProvider,
                         @Param("trackingNumber") String trackingNumber);

    // 【新增】为管理员查询所有订单的方法
    List<Order> findAllForAdmin(@Param("orderId") String orderId, @Param("deliveryMethod") String deliveryMethod);

    long countTotalOrders();
    long countOrdersByStatus(@Param("status") String status);
    List<DailyStatsDTO> countOrderTrends(@Param("days") int days);
    List<String> findExpiredPendingPaymentIds(@Param("limit") int limit);

}
