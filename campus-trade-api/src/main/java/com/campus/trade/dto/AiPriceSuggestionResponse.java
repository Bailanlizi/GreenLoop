package com.campus.trade.dto;

import java.math.BigDecimal;
import java.util.List;

public class AiPriceSuggestionResponse {
    private BigDecimal suggestedMin;
    private BigDecimal suggestedMax;
    private BigDecimal suggestedPrice;
    private BigDecimal categoryAvg;
    private BigDecimal categoryMin;
    private BigDecimal categoryMax;
    private Long categoryCount;
    private String summary;
    private List<String> tips;

    public BigDecimal getSuggestedMin() { return suggestedMin; }
    public void setSuggestedMin(BigDecimal suggestedMin) { this.suggestedMin = suggestedMin; }
    public BigDecimal getSuggestedMax() { return suggestedMax; }
    public void setSuggestedMax(BigDecimal suggestedMax) { this.suggestedMax = suggestedMax; }
    public BigDecimal getSuggestedPrice() { return suggestedPrice; }
    public void setSuggestedPrice(BigDecimal suggestedPrice) { this.suggestedPrice = suggestedPrice; }
    public BigDecimal getCategoryAvg() { return categoryAvg; }
    public void setCategoryAvg(BigDecimal categoryAvg) { this.categoryAvg = categoryAvg; }
    public BigDecimal getCategoryMin() { return categoryMin; }
    public void setCategoryMin(BigDecimal categoryMin) { this.categoryMin = categoryMin; }
    public BigDecimal getCategoryMax() { return categoryMax; }
    public void setCategoryMax(BigDecimal categoryMax) { this.categoryMax = categoryMax; }
    public Long getCategoryCount() { return categoryCount; }
    public void setCategoryCount(Long categoryCount) { this.categoryCount = categoryCount; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }
}
