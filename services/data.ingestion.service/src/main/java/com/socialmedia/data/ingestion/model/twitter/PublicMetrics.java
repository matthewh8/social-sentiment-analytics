package com.socialmedia.data.ingestion.model.twitter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Twitter API v2 Public Metrics
 * Represents engagement metrics for a tweet
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicMetrics {
    
    @JsonProperty("retweet_count")
    private Long retweetCount;
    
    @JsonProperty("like_count")
    private Long likeCount;
    
    @JsonProperty("reply_count")
    private Long replyCount;
    
    @JsonProperty("quote_count")
    private Long quoteCount;
    
    @JsonProperty("bookmark_count")
    private Long bookmarkCount;
    
    @JsonProperty("impression_count")
    private Long impressionCount;
    
    // Constructors
    public PublicMetrics() {}
    
    // Getters and Setters
    public Long getRetweetCount() { return retweetCount; }
    public void setRetweetCount(Long retweetCount) { this.retweetCount = retweetCount; }
    
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    
    public Long getReplyCount() { return replyCount; }
    public void setReplyCount(Long replyCount) { this.replyCount = replyCount; }
    
    public Long getQuoteCount() { return quoteCount; }
    public void setQuoteCount(Long quoteCount) { this.quoteCount = quoteCount; }
    
    public Long getBookmarkCount() { return bookmarkCount; }
    public void setBookmarkCount(Long bookmarkCount) { this.bookmarkCount = bookmarkCount; }
    
    public Long getImpressionCount() { return impressionCount; }
    public void setImpressionCount(Long impressionCount) { this.impressionCount = impressionCount; }
}