package com.campus.trade.dto;

import java.util.List;

public class RiskCheckResponse {
    private String riskLevel;
    private List<String> reasons;

    public RiskCheckResponse() {}
    public RiskCheckResponse(String riskLevel, List<String> reasons) {
        this.riskLevel = riskLevel;
        this.reasons = reasons;
    }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}
