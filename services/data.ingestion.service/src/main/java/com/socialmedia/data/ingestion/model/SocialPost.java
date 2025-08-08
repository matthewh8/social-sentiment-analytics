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

@Entity
@Table(name = "social_posts", 
       indexes = {
           @Index(name = "idx_platform_timestamp", columnList = "platform, createdAt"),
           @Index(name = "idx_author_platform", columnList = "author, platform"),
           @Index(name = "idx_content_hash", columnList = "contentHash"),
           @Index(name = "idx_external_id_platform", columnList = "externalId, platform", unique = true),
           @Index(name = "idx_engagement_score", columnList = "engagementScore"),
           @Index(name = "idx_created_at", columnList = "createdAt"),
           @Index(name = "idx_ingestion_time", columnList = "ingestedAt")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_external_id_platform", columnNames = {"externalId", "platform"})
       })
public class SocialPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Platform cannot be null")
    private Platform platform;
    
    @Column(name = "external_id", nullable = false, length = 100)
    @NotNull(message = "External ID cannot be null")
    @Size(min = 1, max = 100, message = "External ID must be between 1 and 100 characters")
    private String externalId;
    
    @Column(length = 500)
    @Size(max = 500, message = "Title must be less than 500 characters")
    private String title;
    
    @Column(columnDefinition = "TEXT")
    @NotNull(message = "Content cannot be null")
    @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
    private String content;
    
    @Column(nullable = false, length = 100)
    @NotNull(message = "Author cannot be null")
    @Size(min = 1, max = 100, message = "Author name must be between 1 and 100 characters")
    private String author;
    
    @Column(name = "author_id", length = 100)
    @Size(max = 100, message = "Author ID must be less than 100 characters")
    private String authorId;
    
    @Column(length = 500)
    @Size(max = 500, message = "URL must be less than 500 characters")
    private String url;
    
    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created date cannot be null")
    private LocalDateTime createdAt;
    
    @Column(name = "ingested_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;
    
    // Multi-platform engagement metrics
    @Column(name = "upvotes")
    private Integer upvotes = 0;
    
    @Column(name = "downvotes")
    private Integer downvotes = 0;
    
    @Column(name = "comments_count")
    private Integer commentsCount = 0;
    
    @Column(name = "shares_count")
    private Integer sharesCount = 0;
    
    @Column(name = "likes_count")
    private Integer likesCount = 0;
    
    @Column(name = "views_count")
    private Long viewsCount = 0L;
    
    @Column(name = "engagement_score")
    private Double engagementScore = 0.0;
    
    // Platform-specific fields
    @Column(name = "subreddit", length = 100)
    private String subreddit; // Reddit
    
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
    
    // Relationship to sentiment data
    @OneToOne(mappedBy = "socialPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SentimentData sentimentData;
    
    // Constructors
    public SocialPost() {
        this.ingestedAt = LocalDateTime.now();
    }
    
    public SocialPost(Platform platform, String externalId, String content, String author) {
        this();
        this.platform = platform;
        this.externalId = externalId;
        this.content = content;
        this.author = author;
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
        if (content == null) return null;
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
            case TWITTER:
                score = calculateTwitterEngagement();
                break;
            case YOUTUBE:
                score = calculateYouTubeEngagement();
                break;
        }
        
        // Normalize by content length (longer content gets slightly lower scores)
        double contentFactor = Math.max(0.5, 1.0 - (content.length() / 10000.0));
        this.engagementScore = Math.round(score * contentFactor * 100.0) / 100.0;
    }
    
    private double calculateRedditEngagement() {
        double score = 0.0;
        if (upvotes != null) score += upvotes * 1.0;
        if (downvotes != null) score -= downvotes * 0.5;
        if (commentsCount != null) score += commentsCount * 2.0;
        return Math.max(0, score);
    }
    
    private double calculateTwitterEngagement() {
        double score = 0.0;
        if (likesCount != null) score += likesCount * 0.5;
        if (commentsCount != null) score += commentsCount * 2.0;
        if (sharesCount != null) score += sharesCount * 3.0;
        return score;
    }
    
    private double calculateYouTubeEngagement() {
        double score = 0.0;
        if (likesCount != null) score += likesCount * 1.0;
        if (commentsCount != null) score += commentsCount * 2.5;
        if (viewsCount != null) score += viewsCount * 0.01;
        return score;
    }
    
    /**
     * Check if this post is a duplicate of another
     */
    public boolean isDuplicateOf(SocialPost other) {
        return Objects.equals(this.contentHash, other.contentHash) && 
               Objects.equals(this.platform, other.platform);
    }
    
    /**
     * Check if post has high engagement
     */
    public boolean isHighEngagement() {
        return engagementScore != null && engagementScore > 100.0;
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
    public void setContent(String content) { 
        this.content = content;
        if (content != null) {
            this.contentHash = generateContentHash(content);
        }
    }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    
    public Integer getUpvotes() { return upvotes; }
    public void setUpvotes(Integer upvotes) { this.upvotes = upvotes; }
    
    public Integer getDownvotes() { return downvotes; }
    public void setDownvotes(Integer downvotes) { this.downvotes = downvotes; }
    
    public Integer getCommentsCount() { return commentsCount; }
    public void setCommentsCount(Integer commentsCount) { this.commentsCount = commentsCount; }
    
    public Integer getSharesCount() { return sharesCount; }
    public void setSharesCount(Integer sharesCount) { this.sharesCount = sharesCount; }
    
    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
    
    public Long getViewsCount() { return viewsCount; }
    public void setViewsCount(Long viewsCount) { this.viewsCount = viewsCount; }
    
    public Double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public Set<String> getHashtags() { return hashtags; }
    public void setHashtags(Set<String> hashtags) { this.hashtags = hashtags; }
    
    public Set<String> getMentions() { return mentions; }
    public void setMentions(Set<String> mentions) { this.mentions = mentions; }
    
    public Set<String> getTopicTags() { return topicTags; }
    public void setTopicTags(Set<String> topicTags) { this.topicTags = topicTags; }
    
    public SentimentData getSentimentData() { return sentimentData; }
    public void setSentimentData(SentimentData sentimentData) { this.sentimentData = sentimentData; }
    
    // Legacy compatibility methods
    @Deprecated
    public String getPostId() { return externalId; }
    @Deprecated
    public void setPostId(String postId) { this.externalId = postId; }
    
    @Deprecated
    public Integer getScore() { return upvotes; }
    @Deprecated
    public void setScore(Integer score) { this.upvotes = score; }
    
    @Deprecated
    public Integer getCommentCount() { return commentsCount; }
    @Deprecated
    public void setCommentCount(Integer commentCount) { this.commentsCount = commentCount; }
    
    @Deprecated
    public LocalDateTime getIngestionTime() { return ingestedAt; }
    @Deprecated
    public void setIngestionTime(LocalDateTime ingestionTime) { this.ingestedAt = ingestionTime; }
    
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
                ", engagementScore=" + engagementScore +
                ", createdAt=" + createdAt +
                '}';
    }
}