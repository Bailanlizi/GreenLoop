package com.campus.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recommend.hybrid")
public class HybridRecommendationProperties {
    private double embeddingWeight = 0.7;
    private double cfWeight = 0.3;
    private int candidateLimit = 50;
    private int resultLimit = 5;

    public double getEmbeddingWeight() { return embeddingWeight; }
    public void setEmbeddingWeight(double embeddingWeight) { this.embeddingWeight = embeddingWeight; }
    public double getCfWeight() { return cfWeight; }
    public void setCfWeight(double cfWeight) { this.cfWeight = cfWeight; }
    public int getCandidateLimit() { return candidateLimit; }
    public void setCandidateLimit(int candidateLimit) { this.candidateLimit = candidateLimit; }
    public int getResultLimit() { return resultLimit; }
    public void setResultLimit(int resultLimit) { this.resultLimit = resultLimit; }
}
