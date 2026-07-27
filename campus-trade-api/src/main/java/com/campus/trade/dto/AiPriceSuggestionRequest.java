package com.campus.trade.dto;

import java.math.BigDecimal;
import java.util.List;

public class AiPriceSuggestionRequest {
    private String title;
    private String description;
    private Integer categoryId;
    private Integer conditionLevel;
    private List<String> deliveryOptions;
    private BigDecimal currentPrice;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }
    public List<String> getDeliveryOptions() { return deliveryOptions; }
    public void setDeliveryOptions(List<String> deliveryOptions) { this.deliveryOptions = deliveryOptions; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
}
