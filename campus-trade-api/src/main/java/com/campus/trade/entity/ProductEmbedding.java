package com.campus.trade.entity;

import java.util.Date;

public class ProductEmbedding {
    private String productId;
    private String embedding;
    private String model;
    private Date updatedAt;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
