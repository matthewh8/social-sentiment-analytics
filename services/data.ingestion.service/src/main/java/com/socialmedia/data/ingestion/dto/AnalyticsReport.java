
package com.socialmedia.data.ingestion.dto;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Analytics report
 */
public class AnalyticsReport {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime periodStart;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime periodEnd;
    
    // Overall metrics
    private Long totalPosts;
    private Long totalAuthors;
    private Double averageEngagementScore;
    
    // Platform breakdown
    private Map<Platform, Long> postsByPlatform;
    private Map<Platform, Double> avgEngagementByPlatform;
    
    // Sentiment analysis
    private Map<SentimentLabel, Long> sentimentDistribution;
    private Double overallSentimentScore;
    private Map<Platform, Double> sentimentByPlatform;

    
    // Engagement statistics
    private EngagementStats engagementStats;
    
    // Top performers
    private List<TopAuthor> topAuthors;
    private List<TopContent> topPosts;
    private List<String> topHashtags;
    private List<String> topTopics;
    // Top performers (simplified)
    private List<TopAuthor> topAuthors;
    private List<TopPost> topPosts;
    
    // Reddit-specific metrics
    private List<SubredditStats> topSubreddits;
    
    // Time-based trends
    private List<TrendPoint> sentimentTrend;
    private List<TrendPoint> volumeTrend;
    
    // Constructors
    public AnalyticsReport() {
        this.generatedAt = LocalDateTime.now();
    }
    
    public AnalyticsReport(LocalDateTime periodStart, LocalDateTime periodEnd) {
        this();
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
    
    // Nested classes for complex data structures
    public static class TopAuthor {
        private String username;
        private Long postCount;
        private Double avgEngagementScore;
        private Platform primaryPlatform;
        
        public TopAuthor(String username, Long postCount, Double avgEngagementScore, Platform primaryPlatform) {
            this.username = username;
            this.postCount = postCount;
            this.avgEngagementScore = avgEngagementScore;
            this.primaryPlatform = primaryPlatform;
        }
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
        
        public Double getAvgEngagementScore() { return avgEngagementScore; }
        public void setAvgEngagementScore(Double avgEngagementScore) { this.avgEngagementScore = avgEngagementScore; }
        
        public Platform getPrimaryPlatform() { return primaryPlatform; }
        public void setPrimaryPlatform(Platform primaryPlatform) { this.primaryPlatform = primaryPlatform; }
    }
    

    public static class TopPost {
        private Long id;
        private String title;
        private String content;
        private String author;
        private Platform platform;
        private Double engagementScore;
        
        public TopPost(Long id, String title, String content, String author, Platform platform, Double engagementScore) {
            this.id = id;
            this.title = title;
            // Truncate content for display
            this.content = content != null && content.length() > 150 ? 
                content.substring(0, 150) + "..." : content;
            this.author = author;
            this.platform = platform;
            this.engagementScore = engagementScore;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public Platform getPlatform() { return platform; }
        public void setPlatform(Platform platform) { this.platform = platform; }
        
        public Double getEngagementScore() { return engagementScore; }
        public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }
        
        public SentimentLabel getSentiment() { return sentiment; }
        public void setSentiment(SentimentLabel sentiment) { this.sentiment = sentiment; }
    }
    
    public static class SubredditStats {
        private String subreddit;
        private Long postCount;
        private Double avgEngagementScore;
        private Map<SentimentLabel, Long> sentimentBreakdown;
        
        public SubredditStats(String subreddit, Long postCount, Double avgEngagementScore) {
            this.subreddit = subreddit;
            this.postCount = postCount;
            this.avgEngagementScore = avgEngagementScore;
        }
        
        // Getters and Setters
        public String getSubreddit() { return subreddit; }
        public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
        
        public Double getAvgEngagementScore() { return avgEngagementScore; }
        public void setAvgEngagementScore(Double avgEngagementScore) { this.avgEngagementScore = avgEngagementScore; }
        
        public Map<SentimentLabel, Long> getSentimentBreakdown() { return sentimentBreakdown; }
        public void setSentimentBreakdown(Map<SentimentLabel, Long> sentimentBreakdown) { this.sentimentBreakdown = sentimentBreakdown; }
    }
    
    public static class TrendPoint {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;
        private Double value;
        private Long count;
        
        public TrendPoint(LocalDateTime timestamp, Double value, Long count) {
            this.timestamp = timestamp;
            this.value = value;
            this.count = count;
        }
        
        // Getters and Setters
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }

    }
    
    // Getters and Setters for main class
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    
    public LocalDateTime getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    
    public Long getTotalPosts() { return totalPosts; }
    public void setTotalPosts(Long totalPosts) { this.totalPosts = totalPosts; }
    
    public Long getTotalAuthors() { return totalAuthors; }
    public void setTotalAuthors(Long totalAuthors) { this.totalAuthors = totalAuthors; }
    
    public Double getAverageEngagementScore() { return averageEngagementScore; }
    public void setAverageEngagementScore(Double averageEngagementScore) { this.averageEngagementScore = averageEngagementScore; }
    
    public Map<Platform, Long> getPostsByPlatform() { return postsByPlatform; }
    public void setPostsByPlatform(Map<Platform, Long> postsByPlatform) { this.postsByPlatform = postsByPlatform; }
    
    public Map<Platform, Double> getAvgEngagementByPlatform() { return avgEngagementByPlatform; }
    public void setAvgEngagementByPlatform(Map<Platform, Double> avgEngagementByPlatform) { this.avgEngagementByPlatform = avgEngagementByPlatform; }
    
    public Map<SentimentLabel, Long> getSentimentDistribution() { return sentimentDistribution; }
    public void setSentimentDistribution(Map<SentimentLabel, Long> sentimentDistribution) { this.sentimentDistribution = sentimentDistribution; }
    
    public Double getOverallSentimentScore() { return overallSentimentScore; }
    public void setOverallSentimentScore(Double overallSentimentScore) { this.overallSentimentScore = overallSentimentScore; }
    
    public Map<Platform, Double> getSentimentByPlatform() { return sentimentByPlatform; }
    public void setSentimentByPlatform(Map<Platform, Double> sentimentByPlatform) { this.sentimentByPlatform = sentimentByPlatform; }

    public EngagementStats getEngagementStats() { return engagementStats; }
    public void setEngagementStats(EngagementStats engagementStats) { this.engagementStats = engagementStats; }
    
    public List<TopAuthor> getTopAuthors() { return topAuthors; }
    public void setTopAuthors(List<TopAuthor> topAuthors) { this.topAuthors = topAuthors; }
    
    public List<TopContent> getTopPosts() { return topPosts; }
    public void setTopPosts(List<TopContent> topPosts) { this.topPosts = topPosts; }
    
    public List<String> getTopHashtags() { return topHashtags; }
    public void setTopHashtags(List<String> topHashtags) { this.topHashtags = topHashtags; }
    
    public List<String> getTopTopics() { return topTopics; }
    public void setTopTopics(List<String> topTopics) { this.topTopics = topTopics; }
    
    public List<SubredditStats> getTopSubreddits() { return topSubreddits; }
    public void setTopSubreddits(List<SubredditStats> topSubreddits) { this.topSubreddits = topSubreddits; }
    
    public List<TrendPoint> getSentimentTrend() { return sentimentTrend; }
    public void setSentimentTrend(List<TrendPoint> sentimentTrend) { this.sentimentTrend = sentimentTrend; }
    
    public List<TrendPoint> getVolumeTrend() { return volumeTrend; }
    public void setVolumeTrend(List<TrendPoint> volumeTrend) { this.volumeTrend = volumeTrend; }
    public List<TopPost> getTopPosts() { return topPosts; }
    public void setTopPosts(List<TopPost> topPosts) { this.topPosts = topPosts; }
    
    public List<SubredditStats> getTopSubreddits() { return topSubreddits; }
    public void setTopSubreddits(List<SubredditStats> topSubreddits) { this.topSubreddits = topSubreddits; }
}