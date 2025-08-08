package com.socialmedia.data.ingestion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "social_posts", indexes = {
    @Index(name = "idx_post_platform", columnList = "postId, platform"),
    @Index(name = "idx_ingestion_time", columnList = "ingestionTime"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_platform", columnList = "platform")
})
public class SocialPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String postId;
    
    @Column(nullable = false, length = 20)
    private String platform;  // REDDIT, TWITTER, etc.
    
    @Column(length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 100)
    private String author;
    
    @Column(length = 500)
    private String url;
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime ingestionTime;
    
    @Column
    private LocalDateTime processedAt;
    
    // Reddit-specific fields
    @Column
    private Integer score;
    
    @Column
    private Integer commentCount;
    
    @Column(length = 100)
    private String subreddit;
    
    // Sentiment analysis results (will be populated by sentiment service)
    @Column(length = 20)
    private String sentimentLabel;  // POSITIVE, NEGATIVE, NEUTRAL
    
    @Column
    private Double sentimentScore;  // -1.0 to 1.0
    
    @Column
    private Double confidenceScore; // 0.0 to 1.0
    
    // Constructors
    public SocialPost() {}
    
    public SocialPost(String postId, String platform, String title, String content) {
        this.postId = postId;
        this.platform = platform;
        this.title = title;
        this.content = content;
        this.ingestionTime = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getIngestionTime() { return ingestionTime; }
    public void setIngestionTime(LocalDateTime ingestionTime) { this.ingestionTime = ingestionTime; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public String getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(String sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    
    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    @Override
    public String toString() {
        return "SocialPost{" +
                "id=" + id +
                ", postId='" + postId + '\'' +
                ", platform='" + platform + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", subreddit='" + subreddit + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}