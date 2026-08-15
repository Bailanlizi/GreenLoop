package com.campus.trade.dto;

import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class RechargeRequest {
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    @Digits(integer = 16, fraction = 2, message = "充值金额最多保留两位小数")
    private BigDecimal amount;

    @NotBlank(message = "请求号不能为空")
    private String requestId;
}
