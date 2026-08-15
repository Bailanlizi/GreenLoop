package com.campus.trade.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class AccountFreezeRecord {
    private String id;
    private String freezeNo;
    private String orderId;
    private String paymentNo;
    private String accountId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private Date createTime;
    private Date updateTime;
}
