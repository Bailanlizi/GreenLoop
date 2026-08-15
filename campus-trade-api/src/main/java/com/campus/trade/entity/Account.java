package com.campus.trade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Account {
    private String id;
    private String userId;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private String status;
    private Integer version;
    private Date createTime;
    private Date updateTime;
    private String username;
    private String nickname;
}
