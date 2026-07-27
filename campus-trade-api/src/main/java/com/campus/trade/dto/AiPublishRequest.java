package com.campus.trade.dto;

import java.math.BigDecimal;
import java.util.List;

public class AiPublishRequest {
    private String title;
    private String description;
    private Integer categoryId;
    private BigDecimal price;
    private Integer conditionLevel;
    private List<String> deliveryOptions;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getConditionLevel() { return conditionLevel; }
    public void setConditionLevel(Integer conditionLevel) { this.conditionLevel = conditionLevel; }
    public List<String> getDeliveryOptions() { return deliveryOptions; }
    public void setDeliveryOptions(List<String> deliveryOptions) { this.deliveryOptions = deliveryOptions; }
}
