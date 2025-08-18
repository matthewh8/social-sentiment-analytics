package com.socialmedia.data.ingestion.dto;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Search criteria
 */
public class PostSearchCriteria {
    
    // Platform filtering
    private List<Platform> platforms;
    
    // Time range filtering
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;
    
    // Content filtering
    private String contentKeyword;
    
    // Author filtering
    private String author;
    
    // Platform-specific filtering
    private String subreddit;
    
    // Basic engagement filtering
    private Long minUpvotes;
    private Long minLikeCount;
    private Double minEngagementScore;
    
    // Sentiment filtering
    private List<SentimentLabel> sentimentLabels;
    private Double minSentimentScore;
    
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
        
        public PostSearchCriteriaBuilder contentKeyword(String keyword) {
            criteria.contentKeyword = keyword;
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
        return true;
    }
    
    // Getters and Setters
    public List<Platform> getPlatforms() { return platforms; }
    public void setPlatforms(List<Platform> platforms) { this.platforms = platforms; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public String getContentKeyword() { return contentKeyword; }
    public void setContentKeyword(String contentKeyword) { this.contentKeyword = contentKeyword; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public Long getMinUpvotes() { return minUpvotes; }
    public void setMinUpvotes(Long minUpvotes) { this.minUpvotes = minUpvotes; }
    
    public Long getMinLikeCount() { return minLikeCount; }
    public void setMinLikeCount(Long minLikeCount) { this.minLikeCount = minLikeCount; }
    
    public Double getMinEngagementScore() { return minEngagementScore; }
    public void setMinEngagementScore(Double minEngagementScore) { this.minEngagementScore = minEngagementScore; }
    
    public List<SentimentLabel> getSentimentLabels() { return sentimentLabels; }
    public void setSentimentLabels(List<SentimentLabel> sentimentLabels) { this.sentimentLabels = sentimentLabels; }
    
    public Double getMinSentimentScore() { return minSentimentScore; }
    public void setMinSentimentScore(Double minSentimentScore) { this.minSentimentScore = minSentimentScore; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public SortDirection getSortDirection() { return sortDirection; }
    public void setSortDirection(SortDirection sortDirection) { this.sortDirection = sortDirection; }
}