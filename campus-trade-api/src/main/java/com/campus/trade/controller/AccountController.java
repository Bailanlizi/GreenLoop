package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.PageResult;
import com.campus.trade.dto.RechargeRequest;
import com.campus.trade.entity.Account;
import com.campus.trade.entity.AccountFlow;
import com.campus.trade.entity.RechargeOrder;
import com.campus.trade.exception.CustomException;
import com.campus.trade.security.AuthenticatedUser;
import com.campus.trade.service.FinanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/account")
public class AccountController {
    private final FinanceService financeService;

    public AccountController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public Result<Account> getAccount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(financeService.getAccount(userId(user)));
    }

    @GetMapping("/flows")
    public Result<PageResult<AccountFlow>> getFlows(
            @RequestParam(required = false) String businessType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(financeService.getFlows(userId(user), businessType, page, size));
    }

    @GetMapping("/recharges")
    public Result<PageResult<RechargeOrder>> getRecharges(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(financeService.getRecharges(userId(user), page, size));
    }

    @PostMapping("/recharges")
    public Result<RechargeOrder> recharge(@Valid @RequestBody RechargeRequest request,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(financeService.recharge(userId(user), request));
    }

    private String userId(AuthenticatedUser user) {
        if (user == null) throw new CustomException("用户未登录");
        return user.getUserId();
    }
}
