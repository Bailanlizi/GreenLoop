package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.PageResult;
import com.campus.trade.entity.Order;
import com.campus.trade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @Autowired
    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Result<PageResult<Order>> getAllOrders(
            // 【修改】增加 deliveryMethod 请求参数
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(orderService.findAllOrdersForAdmin(orderId, deliveryMethod, page, size));
    }

    @PostMapping("/{id}/force-cancel")
    public Result<Order> forceCancelOrder(@PathVariable String id) {
        return Result.success(orderService.forceCancelOrder(id));
    }
}
