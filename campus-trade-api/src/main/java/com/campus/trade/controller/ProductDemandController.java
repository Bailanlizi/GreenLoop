package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.dto.ProductDemandRequest;
import com.campus.trade.entity.ProductDemand;
import com.campus.trade.exception.CustomException;
import com.campus.trade.security.AuthenticatedUser;
import com.campus.trade.service.ProductDemandService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/demands")
@PreAuthorize("isAuthenticated()")
public class ProductDemandController {

    private final ProductDemandService productDemandService;

    public ProductDemandController(ProductDemandService productDemandService) {
        this.productDemandService = productDemandService;
    }

    private String getUserId(AuthenticatedUser user) {
        if (user == null) {
            throw new CustomException("用户未登录");
        }
        return user.getUserId();
    }

    @PostMapping
    public Result<ProductDemand> create(@RequestBody ProductDemandRequest request,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(productDemandService.createDemand(request, getUserId(user)));
    }

    @GetMapping("/my")
    public Result<List<ProductDemand>> getMyDemands(@AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(productDemandService.getUserDemands(getUserId(user)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        productDemandService.deleteDemand(id, getUserId(user));
        return Result.success();
    }
}
