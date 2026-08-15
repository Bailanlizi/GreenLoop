package com.campus.trade.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentOrder {
    private String id;
    private String paymentNo;
    private String orderId;
    private String buyerId;
    private String requestId;
    private BigDecimal amount;
    private String status;
    private Date createTime;
    private Date paidTime;
    private Date updateTime;
    private String buyerNickname;
}
