package com.socialmedia.data.ingestion.dto;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class PostSearchCriteria {
    
    // Platform filtering (Reddit/YouTube only)
    private List<Platform> platforms;
    
    // Time range filtering
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    
    // Content filtering (both platforms have titles in 2025)
    private String titleKeyword;
    private String contentKeyword;
    private List<String> hashtags;
    private List<String> mentions;
    private List<String> topics;
    
    // Author filtering
    private String author;
    private List<String> authors;
    
    // Platform-specific filtering
    private String subreddit; // Reddit only
    private List<String> subreddits; // Reddit only
    private String videoId; // YouTube only
    
    // Engagement filtering (no downvotes as of 2025)
    private Integer minUpvotes; // Reddit only
    private Integer maxUpvotes; // Reddit only
    private Long minLikesCount; // YouTube only
    private Long maxLikesCount; // YouTube only
    private Long minViewCount; // YouTube only
    private Long maxViewCount; // YouTube only
    private Double minEngagementScore;
    private Double maxEngagementScore;
    
    // Sentiment filtering
    private List<SentimentLabel> sentimentLabels;
    private Double minSentimentScore;
    private Double maxSentimentScore;
    private Double minConfidence;
    
    // Pagination and sorting
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private SortDirection sortDirection = SortDirection.DESC;
    
    public enum SortDirection {
        ASC, DESC
    }
    
    // Constructors
    public PostSearchCriteria() {}
    
    // Builder pattern
    public static PostSearchCriteriaBuilder builder() {
        return new PostSearchCriteriaBuilder();
    }
    
    public static class PostSearchCriteriaBuilder {
        private final PostSearchCriteria criteria = new PostSearchCriteria();
        
        public PostSearchCriteriaBuilder platforms(List<Platform> platforms) {
            criteria.platforms = platforms;
            return this;
        }
        
        public PostSearchCriteriaBuilder platform(Platform platform) {
            criteria.platforms = List.of(platform);
            return this;
        }
        
        public PostSearchCriteriaBuilder dateRange(LocalDateTime start, LocalDateTime end) {
            criteria.startDate = start;
            criteria.endDate = end;
            return this;
        }
        
        public PostSearchCriteriaBuilder titleKeyword(String keyword) {
            criteria.titleKeyword = keyword;
            return this;
        }
        
        public PostSearchCriteriaBuilder contentKeyword(String keyword) {
            criteria.contentKeyword = keyword;
            return this;
        }
        
        public PostSearchCriteriaBuilder hashtags(List<String> hashtags) {
            criteria.hashtags = hashtags;
            return this;
        }
        
        public PostSearchCriteriaBuilder author(String author) {
            criteria.author = author;
            return this;
        }
        
        public PostSearchCriteriaBuilder subreddit(String subreddit) {
            criteria.subreddit = subreddit;
            return this;
        }
        
        public PostSearchCriteriaBuilder videoId(String videoId) {
            criteria.videoId = videoId;
            return this;
        }
        
        public PostSearchCriteriaBuilder minEngagement(Double minScore) {
            criteria.minEngagementScore = minScore;
            return this;
        }
        
        public PostSearchCriteriaBuilder sentimentLabels(List<SentimentLabel> labels) {
            criteria.sentimentLabels = labels;
            return this;
        }
        
        public PostSearchCriteriaBuilder pagination(int page, int size) {
            criteria.page = page;
            criteria.size = size;
            return this;
        }
        
        public PostSearchCriteriaBuilder sortBy(String field, SortDirection direction) {
            criteria.sortBy = field;
            criteria.sortDirection = direction;
            return this;
        }
        
        public PostSearchCriteria build() {
            return criteria;
        }
    }
    
    // Validation method
    public boolean isValid() {
        if (page < 0 || size < 1 || size > 1000) {
            return false;
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return false;
        }
        // Validate platform-specific criteria
        if (platforms != null) {
            for (Platform platform : platforms) {
                if (platform == Platform.REDDIT && videoId != null) {
                    return false; // Reddit doesn't have video IDs
                }
                if (platform == Platform.YOUTUBE && (subreddit != null || subreddits != null)) {
                    return false; // YouTube doesn't have subreddits
                }
            }
        }
        return true;
    }
    
    // Getters and Setters
    public List<Platform> getPlatforms() { return platforms; }
    public void setPlatforms(List<Platform> platforms) { this.platforms = platforms; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getTitleKeyword() { return titleKeyword; }
    public void setTitleKeyword(String titleKeyword) { this.titleKeyword = titleKeyword; }
    
    public String getContentKeyword() { return contentKeyword; }
    public void setContentKeyword(String contentKeyword) { this.contentKeyword = contentKeyword; }
    
    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }
    
    public List<String> getMentions() { return mentions; }
    public void setMentions(List<String> mentions) { this.mentions = mentions; }
    
    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public List<String> getSubreddits() { return subreddits; }
    public void setSubreddits(List<String> subreddits) { this.subreddits = subreddits; }
    
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    
    public Integer getMinUpvotes() { return minUpvotes; }
    public void setMinUpvotes(Integer minUpvotes) { this.minUpvotes = minUpvotes; }
    
    public Integer getMaxUpvotes() { return maxUpvotes; }
    public void setMaxUpvotes(Integer maxUpvotes) { this.maxUpvotes = maxUpvotes; }
    
    public Long getMinLikesCount() { return minLikesCount; }
    public void setMinLikesCount(Long minLikesCount) { this.minLikesCount = minLikesCount; }
    
    public Long getMaxLikesCount() { return maxLikesCount; }
    public void setMaxLikesCount(Long maxLikesCount) { this.maxLikesCount = maxLikesCount; }
    
    public Long getMinViewCount() { return minViewCount; }
    public void setMinViewCount(Long minViewCount) { this.minViewCount = minViewCount; }
    
    public Long getMaxViewCount() { return maxViewCount; }
    public void setMaxViewCount(Long maxViewCount) { this.maxViewCount = maxViewCount; }
    
    public Double getMinEngagementScore() { return minEngagementScore; }
    public void setMinEngagementScore(Double minEngagementScore) { this.minEngagementScore = minEngagementScore; }
    
    public Double getMaxEngagementScore() { return maxEngagementScore; }
    public void setMaxEngagementScore(Double maxEngagementScore) { this.maxEngagementScore = maxEngagementScore; }
    
    public List<SentimentLabel> getSentimentLabels() { return sentimentLabels; }
    public void setSentimentLabels(List<SentimentLabel> sentimentLabels) { this.sentimentLabels = sentimentLabels; }
    
    public Double getMinSentimentScore() { return minSentimentScore; }
    public void setMinSentimentScore(Double minSentimentScore) { this.minSentimentScore = minSentimentScore; }
    
    public Double getMaxSentimentScore() { return maxSentimentScore; }
    public void setMaxSentimentScore(Double maxSentimentScore) { this.maxSentimentScore = maxSentimentScore; }
    
    public Double getMinConfidence() { return minConfidence; }
    public void setMinConfidence(Double minConfidence) { this.minConfidence = minConfidence; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public SortDirection getSortDirection() { return sortDirection; }
    public void setSortDirection(SortDirection sortDirection) { this.sortDirection = sortDirection; }
}