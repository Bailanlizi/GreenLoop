package com.campus.trade.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class PaymentRequest {
    @NotBlank(message = "请求号不能为空")
    private String requestId;
}
