package com.socialmedia.data.ingestion.dto;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class SocialPostDto {
    
    private Long id;
    
    @NotBlank(message = "External ID is required")
    @Size(max = 255, message = "External ID must not exceed 255 characters")
    private String externalId;
    
    @NotNull(message = "Platform is required")
    private Platform platform;
    
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title; // Reddit/YouTube titles (null for Twitter)
    
    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;
    
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")

    private String author;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "Created timestamp is required")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ingestedAt;
    
    // Engagement metrics
    @Min(value = 0, message = "Upvotes must be non-negative")
    private Long upvotes = 0L;
    
    @Min(value = 0, message = "Downvotes must be non-negative")
    private Long downvotes = 0L;
    
    @Min(value = 0, message = "Like count must be non-negative")
    private Long likeCount = 0L;
    
    @Min(value = 0, message = "Share count must be non-negative")
    private Long shareCount = 0L;
    
    @Min(value = 0, message = "Comment count must be non-negative")
    private Long commentCount = 0L;
    
    @DecimalMin(value = "0.0", message = "Engagement score must be non-negative")
    @DecimalMax(value = "100.0", message = "Engagement score must not exceed 100.0")
    private Double engagementScore = 0.0;
    
    // Platform-specific fields
    @Size(max = 100, message = "Subreddit must not exceed 100 characters")
    private String subreddit; // Reddit specific
    
    @Min(value = 0, message = "View count must be non-negative")
    private Long viewCount = 0L; // YouTube specific
    
    @Size(max = 500, message = "Video ID must not exceed 500 characters")
    private String videoId; // YouTube specific
    
    // Content analysis
    private List<String> hashtags;
    private List<String> mentions;
    private List<String> topics;
    
    // Sentiment data (read-only from DTO perspective)
    private SentimentLabel sentimentLabel;
    
    @DecimalMin(value = "0.0", message = "Sentiment score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Sentiment score must be between 0.0 and 1.0")
    private Double sentimentScore;
    
    @DecimalMin(value = "0.0", message = "Confidence must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0.0 and 1.0")
    private Double confidence;
    
    // Constructors
    public SocialPostDto() {
        this.ingestedAt = LocalDateTime.now();
    }
    
    public SocialPostDto(String externalId, Platform platform, String content, String author, LocalDateTime createdAt) {
        this();
        this.externalId = externalId;
        this.platform = platform;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
    }
    
    // Builder pattern for flexible object creation
    public static SocialPostDtoBuilder builder() {
        return new SocialPostDtoBuilder();
    }
    
    public static class SocialPostDtoBuilder {
        private final SocialPostDto dto = new SocialPostDto();
        
        public SocialPostDtoBuilder id(Long id) {
            dto.id = id;
            return this;
        }
        
        public SocialPostDtoBuilder externalId(String externalId) {
            dto.externalId = externalId;
            return this;
        }
        
        public SocialPostDtoBuilder platform(Platform platform) {
            dto.platform = platform;
            return this;
        }
        
        public SocialPostDtoBuilder content(String content) {
            dto.content = content;
            return this;
        }
        
        public SocialPostDtoBuilder author(String author) {
            dto.author = author;
            return this;
        }
        
        public SocialPostDtoBuilder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }
        
        public SocialPostDtoBuilder upvotes(Long upvotes) {
            dto.upvotes = upvotes;
            return this;
        }
        
        public SocialPostDtoBuilder likeCount(Long likeCount) {
            dto.likeCount = likeCount;
            return this;
        }
        
        public SocialPostDtoBuilder subreddit(String subreddit) {
            dto.subreddit = subreddit;
            return this;
        }
        
        public SocialPostDtoBuilder hashtags(List<String> hashtags) {
            dto.hashtags = hashtags;
            return this;
        }
        
        public SocialPostDto build() {
            return dto;
        }
    }
    
    // Getters and Setters (abbreviated for space, but include all fields)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    
    public Long getUpvotes() { return upvotes; }
    public void setUpvotes(Long upvotes) { this.upvotes = upvotes; }
    
    public Long getDownvotes() { return downvotes; }
    public void setDownvotes(Long downvotes) { this.downvotes = downvotes; }
    
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    
    public Long getShareCount() { return shareCount; }
    public void setShareCount(Long shareCount) { this.shareCount = shareCount; }
    
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    
    public Double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    
    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }
    
    public List<String> getMentions() { return mentions; }
    public void setMentions(List<String> mentions) { this.mentions = mentions; }
    
    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
    
    public SentimentLabel getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(SentimentLabel sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    
    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}