package com.campus.trade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AccountFlow {
    private String id;
    private String flowNo;
    private String accountId;
    private String userId;
    private String businessType;
    private String businessNo;
    private BigDecimal availableChange;
    private BigDecimal frozenChange;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;
    private BigDecimal frozenBefore;
    private BigDecimal frozenAfter;
    private String remark;
    private Date createTime;
    private String username;
    private String nickname;
}
