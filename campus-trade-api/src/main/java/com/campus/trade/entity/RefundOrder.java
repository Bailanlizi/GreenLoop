package com.campus.trade.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RefundOrder {
    private String id;
    private String refundNo;
    private String orderId;
    private String paymentNo;
    private String buyerId;
    private BigDecimal amount;
    private String status;
    private Date createTime;
    private Date successTime;
    private String buyerNickname;
}
