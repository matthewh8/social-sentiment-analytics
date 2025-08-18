package com.socialmedia.data.ingestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 *  SocialPost entity
*/
@Entity
@Table(name = "social_posts", 
       indexes = {
           @Index(name = "idx_platform_created", columnList = "platform, createdAt"),
           @Index(name = "idx_external_platform", columnList = "externalId, platform", unique = true),
           @Index(name = "idx_engagement", columnList = "engagementScore")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_external_platform", columnNames = {"externalId", "platform"})
       })
public class SocialPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Core identification
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Platform cannot be null")
    private Platform platform;
    
    @Column(name = "external_id", nullable = false, length = 100)
    @NotNull(message = "External ID cannot be null")
    @Size(min = 1, max = 100, message = "External ID must be between 1 and 100 characters")
    private String externalId;
    
    // Content
    @Column(length = 500) // Reddit titles, YouTube titles (nullable for Twitter)
    @Size(max = 500, message = "Title must be less than 500 characters")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    @NotNull(message = "Content cannot be null")
    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    private String content;
    
    @Column(nullable = false, length = 100)
    @NotNull(message = "Author cannot be null")
    @Size(min = 1, max = 100, message = "Author name must be between 1 and 100 characters")
    private String author;
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created date cannot be null")
    private LocalDateTime createdAt;
    
    @Column(name = "ingested_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
    
    // Universal engagement metrics (work for all platforms)
    @Column(name = "like_count")
    private Long likeCount = 0L;
    
    @Column(name = "comment_count")
    private Long commentCount = 0L;
    
    @Column(name = "share_count")
    private Long shareCount = 0L;
    
    // Platform-specific metrics (only when needed)
    @Column(name = "upvotes") // Reddit only
    private Long upvotes = 0L;
    
    @Column(name = "view_count") // YouTube, TikTok
    private Long viewCount = 0L;
    
    // Calculated field
    @Column(name = "engagement_score")
    private Double engagementScore = 0.0;
    
    // Platform-specific context fields
    @Column(name = "subreddit", length = 100) // Reddit
    private String subreddit;
    
    @Column(name = "video_id", length = 500) // YouTube
    @Size(max = 500, message = "Video ID must be less than 500 characters")
    private String videoId;
    
    // Simplified relationship - no cascades, lazy loading
    @OneToOne(mappedBy = "socialPost", fetch = FetchType.LAZY)
    private SentimentData sentimentData;
    
    // Constructors
    public SocialPost() {
        this.ingestedAt = LocalDateTime.now();
    }
    
    public SocialPost(Platform platform, String externalId, String title, String content, String author, LocalDateTime createdAt) {
        this();
        this.platform = platform;
        this.externalId = externalId;
        this.title = title; // Can be null for Twitter
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters (no business logic here)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    
    public Long getShareCount() { return shareCount; }
    public void setShareCount(Long shareCount) { this.shareCount = shareCount; }
    
    public Long getUpvotes() { return upvotes; }
    public void setUpvotes(Long upvotes) { this.upvotes = upvotes; }
    
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    
    public Double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    
    public SentimentData getSentimentData() { return sentimentData; }
    public void setSentimentData(SentimentData sentimentData) { this.sentimentData = sentimentData; }
    
    // Simple equality based on business key
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocialPost that = (SocialPost) o;
        return Objects.equals(externalId, that.externalId) && 
               platform == that.platform;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(externalId, platform);
    }
    
    @Override
    public String toString() {
        return "SocialPost{" +
                "id=" + id +
                ", platform=" + platform +
                ", externalId='" + externalId + '\'' +
                ", author='" + author + '\'' +
                ", engagementScore=" + engagementScore +
                '}';
    }
}