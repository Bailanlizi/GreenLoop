package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.CreateOrderDTO;
import com.campus.trade.dto.ShipmentDTO;
import com.campus.trade.dto.PaymentRequest;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.PaymentOrder;
import com.campus.trade.exception.CustomException;
import com.campus.trade.security.AuthenticatedUser;
import com.campus.trade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private String getUserId(AuthenticatedUser user) {
        if (user == null) throw new CustomException("用户未登录");
        return user.getUserId();
    }

    @PostMapping
    public Result<Order> createOrder(@Valid @RequestBody CreateOrderDTO createOrderDTO, @AuthenticationPrincipal AuthenticatedUser user) {
        Order order = orderService.createOrder(getUserId(user), createOrderDTO);
        return Result.success(order);
    }

    @GetMapping("/{orderId}")
    public Result<Order> getOrderById(@PathVariable String orderId) {
        Order order = orderService.getOrderDetails(orderId);
        return Result.success(order);
    }

    @GetMapping("/my-purchases")
    public Result<List<Order>> getMyPurchases(@AuthenticationPrincipal AuthenticatedUser user) {
        List<Order> orders = orderService.getMyPurchases(getUserId(user));
        return Result.success(orders);
    }

    @GetMapping("/my-sales")
    public Result<List<Order>> getMySales(@AuthenticationPrincipal AuthenticatedUser user) {
        List<Order> orders = orderService.getMySales(getUserId(user));
        return Result.success(orders);
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Order> cancelOrder(@PathVariable String orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(orderService.cancelOrder(orderId, getUserId(user)));
    }

    @PostMapping("/{orderId}/pay")
    public Result<PaymentOrder> payOrder(@PathVariable String orderId, @Valid @RequestBody PaymentRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(orderService.payOrder(orderId, getUserId(user), request));
    }

    @GetMapping("/{orderId}/payment")
    public Result<PaymentOrder> getPayment(@PathVariable String orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(orderService.getPayment(orderId, getUserId(user)));
    }

    @PostMapping("/{orderId}/confirm-completion")
    public Result<Order> confirmCompletion(@PathVariable String orderId, @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(orderService.confirmCompletion(orderId, getUserId(user)));
    }

    @PostMapping("/{orderId}/ship")
    public Result<Order> shipOrder(@PathVariable String orderId, @RequestBody ShipmentDTO shipmentDTO,
                                   @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(orderService.shipOrder(orderId, getUserId(user), shipmentDTO));
    }
}
