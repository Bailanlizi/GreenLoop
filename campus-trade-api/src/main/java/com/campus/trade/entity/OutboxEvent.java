package com.campus.trade.entity;

import java.util.Date;

public class OutboxEvent implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String eventId;
    private String eventType;
    private String recipientId;
    private String relatedId;
    private String relatedType;
    private String content;
    private String status;
    private Integer retryCount;
    private Date nextRetryTime;
    private String lastError;
    private Date publishedTime;
    private Date createTime;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; } public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; } public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRecipientId() { return recipientId; } public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getRelatedId() { return relatedId; } public void setRelatedId(String relatedId) { this.relatedId = relatedId; }
    public String getRelatedType() { return relatedType; } public void setRelatedType(String relatedType) { this.relatedType = relatedType; }
    public String getContent() { return content; } public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; } public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Date getNextRetryTime() { return nextRetryTime; } public void setNextRetryTime(Date nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public String getLastError() { return lastError; } public void setLastError(String lastError) { this.lastError = lastError; }
    public Date getPublishedTime() { return publishedTime; } public void setPublishedTime(Date publishedTime) { this.publishedTime = publishedTime; }
    public Date getCreateTime() { return createTime; } public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
