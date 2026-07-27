package com.campus.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "search.hybrid")
public class HybridSearchProperties {
    private double semanticWeight = 0.7;
    private double lexicalWeight = 0.3;
    private int candidateLimit = 200;
    private double minSemanticScore = 0.2;

    public double getSemanticWeight() { return semanticWeight; }
    public void setSemanticWeight(double semanticWeight) { this.semanticWeight = semanticWeight; }
    public double getLexicalWeight() { return lexicalWeight; }
    public void setLexicalWeight(double lexicalWeight) { this.lexicalWeight = lexicalWeight; }
    public int getCandidateLimit() { return candidateLimit; }
    public void setCandidateLimit(int candidateLimit) { this.candidateLimit = candidateLimit; }
    public double getMinSemanticScore() { return minSemanticScore; }
    public void setMinSemanticScore(double minSemanticScore) { this.minSemanticScore = minSemanticScore; }
}
