package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.PageResult;
import com.campus.trade.entity.*;
import com.campus.trade.service.FinanceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/finance")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinanceController {
    private final FinanceService financeService;

    public AdminFinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/accounts")
    public Result<PageResult<Account>> accounts(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(financeService.findAccounts(keyword, status, page, size));
    }

    @GetMapping("/payments")
    public Result<PageResult<PaymentOrder>> payments(@RequestParam(required = false) String orderId,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(financeService.findPayments(orderId, status, page, size));
    }

    @GetMapping("/refunds")
    public Result<PageResult<RefundOrder>> refunds(@RequestParam(required = false) String orderId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(financeService.findRefunds(orderId, status, page, size));
    }

    @GetMapping("/settlements")
    public Result<PageResult<SettlementOrder>> settlements(@RequestParam(required = false) String orderId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "1") Integer page,
                                                           @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(financeService.findSettlements(orderId, status, page, size));
    }

    @GetMapping("/flows")
    public Result<PageResult<AccountFlow>> flows(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String businessType,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(financeService.findFlows(keyword, businessType, page, size));
    }
}
