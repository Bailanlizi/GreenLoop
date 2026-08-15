package com.campus.trade.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class RechargeOrder {
    private String id;
    private String rechargeNo;
    private String userId;
    private String requestId;
    private BigDecimal amount;
    private String status;
    private Date createTime;
    private Date successTime;
    private String username;
    private String nickname;
}
