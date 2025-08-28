package com.socialmedia.data.ingestion.model.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeStatistics {
    
    @JsonProperty("viewCount")
    private String viewCount;
    
    @JsonProperty("likeCount")
    private String likeCount;
    
    @JsonProperty("favoriteCount")
    private String favoriteCount;
    
    @JsonProperty("commentCount")
    private String commentCount;
    
    // Constructors
    public YouTubeStatistics() {}
    
    // Getters and setters with Long conversion (YouTube returns strings)
    public String getViewCountRaw() { return viewCount; }
    public void setViewCount(String viewCount) { this.viewCount = viewCount; }
    
    public Long getViewCount() {
        try {
            return viewCount != null ? Long.parseLong(viewCount) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    public String getLikeCountRaw() { return likeCount; }
    public void setLikeCount(String likeCount) { this.likeCount = likeCount; }
    
    public Long getLikeCount() {
        try {
            return likeCount != null ? Long.parseLong(likeCount) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    public String getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(String favoriteCount) { this.favoriteCount = favoriteCount; }
    
    public String getCommentCountRaw() { return commentCount; }
    public void setCommentCount(String commentCount) { this.commentCount = commentCount; }
    
    public Long getCommentCount() {
        try {
            return commentCount != null ? Long.parseLong(commentCount) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}