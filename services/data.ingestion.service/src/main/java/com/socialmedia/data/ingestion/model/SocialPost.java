package com.socialmedia.data.ingestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * SocialPost entity - Reddit and YouTube focused (2025 version)
 * Both platforms require titles as of 2025
 */
@Entity
@Table(name = "social_posts", 
       indexes = {
           @Index(name = "idx_platform_created", columnList = "platform, createdAt"),
           @Index(name = "idx_external_platform", columnList = "externalId, platform", unique = true),
           @Index(name = "idx_engagement", columnList = "engagementScore"),
           @Index(name = "idx_subreddit", columnList = "subreddit"),
           @Index(name = "idx_video_id", columnList = "videoId")
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
    
    // Content - Both Reddit and YouTube have titles (required in 2025)
    @Column(length = 500, nullable = false)
    @NotNull(message = "Title cannot be null")
    @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    @Size(max = 10000, message = "Content must be less than 10000 characters")
    private String content; // Optional for YouTube, required for Reddit
    
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
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    
    // Engagement metrics (no downvotes - removed in 2025)
    @Column(name = "upvotes")
    private Long upvotes = 0L; // Reddit only
    
    @Column(name = "comment_count")
    private Long commentCount = 0L; // Both platforms
    
    @Column(name = "share_count")
    private Long shareCount = 0L; // Both platforms
    
    @Column(name = "like_count")
    private Long likeCount = 0L; // YouTube only
    
    @Column(name = "view_count")
    private Long viewCount = 0L; // YouTube only
    
    // Calculated engagement score
    @Column(name = "engagement_score")
    private Double engagementScore = 0.0;
    
    // Platform-specific context fields
    @Column(name = "subreddit", length = 100) // Reddit only
    private String subreddit;
    
    @Column(name = "video_id", length = 500) // YouTube only
    @Size(max = 500, message = "Video ID must be less than 500 characters")
    private String videoId;
    
    @Column(name = "url", length = 1000) // External URL
    private String url;
    
    // Content analysis collections
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_hashtags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "hashtag")
    private Set<String> hashtags = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_mentions", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "mention")
    private Set<String> mentions = new HashSet<>();
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_topics", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "topic")
    private Set<String> topicTags = new HashSet<>();
    
    // Sentiment relationship
    @OneToOne(mappedBy = "socialPost", fetch = FetchType.LAZY)
    private SentimentData sentimentData;
    
    // Constructors
    public SocialPost() {
        this.ingestedAt = LocalDateTime.now();
    }
    
    public SocialPost(Platform platform, String externalId, String title, String content, String author) {
        this();
        this.platform = platform;
        this.externalId = externalId;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = LocalDateTime.now();
        this.contentHash = generateContentHash(content);
    }
    
    // Business methods
    @PrePersist
    @PreUpdate
    private void generateHashIfNeeded() {
        if (this.contentHash == null && this.content != null) {
            this.contentHash = generateContentHash(this.content);
        }
    }
    
    private String generateContentHash(String content) {
        if (content == null || content.trim().isEmpty()) return null;
        return DigestUtils.md5DigestAsHex(content.toLowerCase().trim().getBytes());
    }
    
    /**
     * Calculate engagement score based on platform-specific metrics
     */
    public void calculateEngagementScore() {
        double score = 0.0;
        
        switch (platform) {
            case REDDIT:
                score = calculateRedditEngagement();
                break;
            case YOUTUBE:
                score = calculateYouTubeEngagement();
                break;
        }
        
        // Normalize by content length factor (longer content might get less engagement)
        double contentFactor = 1.0;
        if (content != null) {
            contentFactor = Math.max(0.5, 1.0 - (content.length() / 20000.0));
        }
        
        this.engagementScore = Math.round(score * contentFactor * 100.0) / 100.0;
    }
    
    private double calculateRedditEngagement() {
        long upvotes = this.upvotes != null ? this.upvotes : 0;
        long comments = this.commentCount != null ? this.commentCount : 0;
        
        // Reddit formula: upvotes + (comments * 2.5) - higher weight for comments
        double score = upvotes + (comments * 2.5);
        
        // Logarithmic scaling for very high scores
        if (score > 1000) {
            score = 1000 + Math.log10(score - 999) * 100;
        }
        
        return Math.max(0, score);
    }
    
    private double calculateYouTubeEngagement() {
        long likes = this.likeCount != null ? this.likeCount : 0;
        long comments = this.commentCount != null ? this.commentCount : 0;
        long views = this.viewCount != null ? this.viewCount : 0;
        
        if (views == 0) {
            // If no views, base score on likes and comments only
            return (likes * 1.5) + (comments * 3.0);
        }
        
        // YouTube formula: engagement rate * views + base engagement
        double engagementRate = ((double) (likes + comments)) / views;
        double baseEngagement = (likes * 1.5) + (comments * 3.0);
        
        // Scale engagement rate and combine with base
        double score = (engagementRate * views * 0.1) + baseEngagement;
        
        return Math.max(0, score);
    }
    
    /**
     * Check if this post is a duplicate of another
     */
    public boolean isDuplicateOf(SocialPost other) {
        return Objects.equals(this.contentHash, other.contentHash) && 
               Objects.equals(this.platform, other.platform);
    }
    
    /**
     * Check if post has high engagement (platform-specific thresholds)
     */
    public boolean isHighEngagement() {
        if (engagementScore == null) return false;
        
        switch (platform) {
            case REDDIT:
                return engagementScore > 500.0; // High threshold for Reddit
            case YOUTUBE:
                return engagementScore > 1000.0; // Higher threshold for YouTube
            default:
                return engagementScore > 100.0;
        }
    }
    
    /**
     * Validate platform-specific requirements
     */
    public boolean isValidForPlatform() {
        switch (platform) {
            case REDDIT:
                return subreddit != null && !subreddit.trim().isEmpty() &&
                       content != null && !content.trim().isEmpty();
            case YOUTUBE:
                return videoId != null && !videoId.trim().isEmpty();
            default:
                return false;
        }
    }
    
    // Getters and Setters
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
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    
    public Long getUpvotes() { return upvotes; }
    public void setUpvotes(Long upvotes) { this.upvotes = upvotes; }
    
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    
    public Long getShareCount() { return shareCount; }
    public void setShareCount(Long shareCount) { this.shareCount = shareCount; }
    
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    
    public Double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public Set<String> getHashtags() { return hashtags; }
    public void setHashtags(Set<String> hashtags) { this.hashtags = hashtags; }
    
    public Set<String> getMentions() { return mentions; }
    public void setMentions(Set<String> mentions) { this.mentions = mentions; }
    
    public Set<String> getTopicTags() { return topicTags; }
    public void setTopicTags(Set<String> topicTags) { this.topicTags = topicTags; }
    
    public SentimentData getSentimentData() { return sentimentData; }
    public void setSentimentData(SentimentData sentimentData) { this.sentimentData = sentimentData; }
    
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
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", subreddit='" + subreddit + '\'' +
                ", videoId='" + videoId + '\'' +
                ", engagementScore=" + engagementScore +
                '}';
    }
}