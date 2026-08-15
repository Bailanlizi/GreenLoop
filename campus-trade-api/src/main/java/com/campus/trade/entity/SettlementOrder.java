package com.campus.trade.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class SettlementOrder {
    private String id;
    private String settlementNo;
    private String orderId;
    private String paymentNo;
    private String buyerId;
    private String sellerId;
    private BigDecimal amount;
    private String status;
    private Date createTime;
    private Date successTime;
    private String buyerNickname;
    private String sellerNickname;
}
