package com.socialmedia.data.ingestion.model.twitter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Twitter API v2 Tweet Response Model
 * Simplified for MVP - only essential fields needed for analytics
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tweet {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("text")
    private String text;
    
    @JsonProperty("author_id")
    private String authorId;
    
    @JsonProperty("created_at")
    private String createdAt; // ISO 8601 format from Twitter API
    
    @JsonProperty("public_metrics")
    private PublicMetrics publicMetrics;
    
    @JsonProperty("context_annotations")
    private Object contextAnnotations; // Simplified - not parsing for MVP
    
    @JsonProperty("lang")
    private String language;
    
    // Constructors
    public Tweet() {}
    
    public Tweet(String id, String text, String authorId, String createdAt) {
        this.id = id;
        this.text = text;
        this.authorId = authorId;
        this.createdAt = createdAt;
    }
    
    // Helper methods for engagement
    public Long getTotalEngagement() {
        if (publicMetrics == null) return 0L;
        
        long total = 0;
        if (publicMetrics.getLikeCount() != null) total += publicMetrics.getLikeCount();
        if (publicMetrics.getRetweetCount() != null) total += publicMetrics.getRetweetCount();
        if (publicMetrics.getReplyCount() != null) total += publicMetrics.getReplyCount();
        if (publicMetrics.getQuoteCount() != null) total += publicMetrics.getQuoteCount();
        
        return total;
    }
    
    public boolean isValidTweet() {
        // Basic validation for analytics purposes
        return id != null && !id.trim().isEmpty() &&
               text != null && !text.trim().isEmpty() &&
               authorId != null && !authorId.trim().isEmpty();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public PublicMetrics getPublicMetrics() { return publicMetrics; }
    public void setPublicMetrics(PublicMetrics publicMetrics) { this.publicMetrics = publicMetrics; }
    
    public Object getContextAnnotations() { return contextAnnotations; }
    public void setContextAnnotations(Object contextAnnotations) { this.contextAnnotations = contextAnnotations; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}