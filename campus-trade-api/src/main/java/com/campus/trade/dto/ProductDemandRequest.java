package com.campus.trade.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductDemandRequest {
    private Integer categoryId;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer conditionLevel;
    private List<String> deliveryOptions;

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }
    public List<String> getDeliveryOptions() { return deliveryOptions; }
    public void setDeliveryOptions(List<String> deliveryOptions) { this.deliveryOptions = deliveryOptions; }
}
