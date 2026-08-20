package com.campus.trade.service.impl;

import com.campus.trade.domain.OrderStatus;
import com.campus.trade.domain.ProductStatus;
import com.campus.trade.dto.CreateOrderDTO;
import com.campus.trade.dto.DeliveryStatsDTO;
import com.campus.trade.dto.PageResult;
import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.dto.ShipmentDTO;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.PaymentOrder;
import com.campus.trade.exception.CustomException;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.NotificationEventService;
import com.campus.trade.service.FinanceService;
import com.campus.trade.service.OrderService;
import com.github.pagehelper.PageHelper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Date;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final NotificationEventService notificationEvents;
    private final FinanceService financeService;

    public OrderServiceImpl(OrderMapper orderMapper, ProductMapper productMapper, NotificationEventService notificationEvents,
                            FinanceService financeService) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.notificationEvents = notificationEvents;
        this.financeService = financeService;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Order createOrder(String buyerId, CreateOrderDTO request) {
        if (productMapper.lockProductIfAvailable(request.getProductId()) != 1) {
            throw new CustomException("商品已被占用或不可交易");
        }

        Product product = productMapper.findProductById(request.getProductId());
        if (product == null) {
            throw new CustomException("商品不存在");
        }
        if (Objects.equals(product.getSellerId(), buyerId)) {
            throw new CustomException("不能购买自己发布的商品");
        }
        if (!supportsDelivery(product, request.getDeliveryMethod())) {
            throw new CustomException("该商品不支持您选择的配送方式");
        }

        Order order = new Order();
        order.setProductId(product.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setTotalPrice(product.getPrice());
        order.setDeliveryMethod(request.getDeliveryMethod());
        if ("MEETUP".equals(request.getDeliveryMethod())) {
            if (request.getMeetupLocationId() == null) {
                throw new CustomException("请选择交易地点");
            }
            order.setMeetupLocationId(request.getMeetupLocationId());
        } else {
            if (request.getShippingAddressId() == null) {
                throw new CustomException("请选择收货地址");
            }
            order.setShippingAddressId(request.getShippingAddressId());
        }

        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setPaymentDeadline(new Date(System.currentTimeMillis() + 30L * 60L * 1000L));

        orderMapper.insertOrder(order);
        notificationEvents.order(order.getSellerId(), "ORDER_CREATED", "有买家下单，等待买家付款。", order.getId());
        return orderMapper.findOrderById(order.getId());
    }

    @Override
    public PaymentOrder payOrder(String orderId, String buyerId, PaymentRequest request) {
        return financeService.payOrder(orderId, buyerId, request);
    }

    @Override
    public PaymentOrder getPayment(String orderId, String userId) {
        return financeService.getPayment(orderId, userId);
    }

    @Override
    @Cacheable(value = "order", key = "#orderId")
    public Order getOrderDetails(String orderId) {
        return orderMapper.findOrderById(orderId);
    }

    @Override
    public List<Order> getMyPurchases(String userId) {
        return orderMapper.findOrdersByBuyerId(userId);
    }

    @Override
    public List<Order> getMySales(String userId) {
        return orderMapper.findOrdersBySellerId(userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"order", "products", "product"}, allEntries = true)
    public Order cancelOrder(String orderId, String buyerId) {
        Order order = requireOrderForUpdate(orderId);
        requireBuyer(order, buyerId);
        OrderStatus currentStatus = OrderStatus.valueOf(order.getOrderStatus());
        if (!currentStatus.canBuyerCancel()) {
            throw new CustomException("当前订单已开始履约，无法取消");
        }
        cancelAndRelease(order, currentStatus);
        return orderMapper.findOrderById(orderId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"order", "products", "product"}, allEntries = true)
    public Order confirmCompletion(String orderId, String buyerId) {
        Order order = requireOrderForUpdate(orderId);
        requireBuyer(order, buyerId);
        OrderStatus currentStatus = OrderStatus.valueOf(order.getOrderStatus());
        if (!currentStatus.canBuyerConfirmCompletion(order.getDeliveryMethod())) {
            throw new CustomException("当前订单状态不允许确认完成");
        }
        financeService.settleOrder(order);
        updateOrderOrThrow(order, currentStatus, OrderStatus.COMPLETED);
        if (productMapper.updateProductStatusIfCurrent(order.getProductId(), ProductStatus.LOCKED.name(), ProductStatus.SOLD.name()) != 1) {
            throw new CustomException("商品状态异常，无法完成订单");
        }
        notificationEvents.order(order.getSellerId(), "ORDER_SETTLED", "买家已确认收货，款项已结算至您的账户。", orderId);
        return orderMapper.findOrderById(orderId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "order", key = "#orderId")
    public Order shipOrder(String orderId, String sellerId, ShipmentDTO shipmentDTO) {
        Order order = requireOrderForUpdate(orderId);
        if (!Objects.equals(order.getSellerId(), sellerId)) {
            throw new CustomException("无权为此订单发货");
        }
        if (shipmentDTO == null || isBlank(shipmentDTO.getShippingProvider()) || isBlank(shipmentDTO.getTrackingNumber())) {
            throw new CustomException("请填写快递公司和快递单号");
        }
        if (orderMapper.markOrderShipped(orderId, shipmentDTO.getShippingProvider().trim(), shipmentDTO.getTrackingNumber().trim()) != 1) {
            throw new CustomException("当前订单状态不允许发货");
        }
        notificationEvents.order(order.getBuyerId(), "ORDER_SHIPPED", "您的订单已发货。", orderId);
        return orderMapper.findOrderById(orderId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"order", "products", "product"}, allEntries = true)
    public Order forceCancelOrder(String orderId) {
        Order order = requireOrderForUpdate(orderId);
        OrderStatus currentStatus = OrderStatus.valueOf(order.getOrderStatus());
        if (!currentStatus.canBuyerCancel()) {
            throw new CustomException("仅未履约订单可强制取消");
        }
        cancelAndRelease(order, currentStatus);
        notificationEvents.order(order.getBuyerId(), "ORDER_CANCELLED", "订单已被平台强制取消。", orderId);
        return orderMapper.findOrderById(orderId);
    }

    @Override
    public PageResult<Order> findAllOrdersForAdmin(String orderId, String deliveryMethod, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        return new PageResult<>(orderMapper.findAllForAdmin(orderId, deliveryMethod));
    }

    @Override
    public DeliveryStatsDTO getDeliveryStats() {
        return new DeliveryStatsDTO(
                orderMapper.countOrdersByStatus(OrderStatus.AWAITING_SHIPMENT.name()),
                orderMapper.countOrdersByStatus(OrderStatus.SHIPPED.name()),
                orderMapper.countOrdersByStatus(OrderStatus.COMPLETED.name()),
                orderMapper.countTotalOrders());
    }

    @Override
    public void exportDeliveryOrders(String orderId, String deliveryMethod, String orderStatus, HttpServletResponse response) throws IOException {
        List<Order> orders = orderMapper.findAllForAdmin(orderId, deliveryMethod);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=delivery_orders.csv");
        StringBuilder csv = new StringBuilder("订单ID,商品标题,买家,卖家,配送方式,订单状态,订单金额\n");
        for (Order order : orders) {
            if (orderStatus != null && !orderStatus.isEmpty() && !orderStatus.equals(order.getOrderStatus())) {
                continue;
            }
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%.2f\n", order.getId(), safe(order.getProductTitle()),
                    safe(order.getBuyerNickname()), safe(order.getSellerNickname()),
                    "SHIPPING".equals(order.getDeliveryMethod()) ? "快递配送" : "线下面交",
                    formatOrderStatus(order.getOrderStatus()), order.getTotalPrice()));
        }
        response.getWriter().write(csv.toString());
    }

    private void cancelAndRelease(Order order, OrderStatus currentStatus) {
        if (currentStatus == OrderStatus.AWAITING_MEETUP || currentStatus == OrderStatus.AWAITING_SHIPMENT) {
            financeService.refundPaidOrder(order);
            notificationEvents.order(order.getBuyerId(), "ORDER_REFUNDED", "订单已取消，冻结资金已退回账户。", order.getId());
        }
        updateOrderOrThrow(order, currentStatus, OrderStatus.CANCELLED);
        if (productMapper.updateProductStatusIfCurrent(order.getProductId(), ProductStatus.LOCKED.name(), ProductStatus.AVAILABLE.name()) != 1) {
            throw new CustomException("商品状态异常，无法释放商品");
        }
        notificationEvents.order(order.getSellerId(), "ORDER_CANCELLED", "订单已取消，商品已恢复可售。", order.getId());
    }

    private void updateOrderOrThrow(Order order, OrderStatus currentStatus, OrderStatus targetStatus) {
        if (orderMapper.updateOrderStatusIfCurrent(order.getId(), currentStatus.name(), targetStatus.name()) != 1) {
            throw new CustomException("订单状态已变化，请刷新后重试");
        }
    }

    private Order requireOrder(String orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        return order;
    }

    private Order requireOrderForUpdate(String orderId) {
        Order order = orderMapper.findOrderByIdForUpdate(orderId);
        if (order == null) throw new CustomException("订单不存在");
        return order;
    }

    private void requireBuyer(Order order, String buyerId) {
        if (!Objects.equals(order.getBuyerId(), buyerId)) {
            throw new CustomException("无权操作此订单");
        }
    }

    private boolean supportsDelivery(Product product, String deliveryMethod) {
        if (product.getDeliveryOptions() == null) {
            return false;
        }
        for (String option : product.getDeliveryOptions().split(",")) {
            if (deliveryMethod.equals(option.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(',', ' ');
    }

    private String formatOrderStatus(String status) {
        switch (OrderStatus.valueOf(status)) {
            case PENDING_PAYMENT: return "待支付";
            case AWAITING_MEETUP: return "待交易";
            case AWAITING_SHIPMENT: return "待发货";
            case SHIPPED: return "已发货";
            case COMPLETED: return "已完成";
            case CANCELLED: return "已取消";
            default: return status;
        }
    }
}
