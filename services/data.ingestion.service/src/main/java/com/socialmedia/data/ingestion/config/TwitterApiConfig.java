package com.socialmedia.data.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Twitter API Configuration
 * Handles Twitter API v2 settings and authentication
 */
@Configuration
@ConfigurationProperties(prefix = "twitter.api")
public class TwitterApiConfig {
    
    private String baseUrl = "https://api.twitter.com/2";
    private String bearerToken;
    private String userAgent = "SocialMediaAnalytics/1.0";
    
    // Rate limiting settings (Twitter API v2 limits)
    private int requestsPerWindow = 300; // 300 requests per 15 minutes
    private int windowSizeMinutes = 15;
    
    // API settings
    private int maxResultsPerRequest = 100; // Twitter API v2 max is 100
    private int defaultTweetCount = 10;
    private String defaultTweetFields = "id,text,author_id,created_at,public_metrics,lang";
    private String defaultUserFields = "id,name,username";
    private String defaultExpansions = "author_id";
    
    // Tweet filtering
    private boolean excludeRetweets = true;
    private boolean excludeReplies = false;
    private String[] defaultLanguages = {"en"};
    
    // Constructors
    public TwitterApiConfig() {}
    
    // Helper methods
    public String getSearchRecentUrl() {
        return baseUrl + "/tweets/search/recent";
    }
    
    public String getUserTweetsUrl(String userId) {
        return baseUrl + "/users/" + userId + "/tweets";
    }
    
    public String getUserByUsernameUrl(String username) {
        return baseUrl + "/users/by/username/" + username;
    }
    
    public boolean isConfigured() {
        return bearerToken != null && !bearerToken.trim().isEmpty();
    }
    
    public String getAuthorizationHeader() {
        return "Bearer " + bearerToken;
    }
    
    // Validation
    public void validateConfiguration() {
        if (!isConfigured()) {
            throw new IllegalStateException("Twitter Bearer Token is required but not configured");
        }
        
        if (requestsPerWindow <= 0 || windowSizeMinutes <= 0) {
            throw new IllegalStateException("Invalid rate limiting configuration");
        }
        
        if (maxResultsPerRequest > 100 || maxResultsPerRequest < 1) {
            throw new IllegalStateException("Max results per request must be between 1 and 100");
        }
    }
    
    // Getters and Setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getBearerToken() { return bearerToken; }
    public void setBearerToken(String bearerToken) { this.bearerToken = bearerToken; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public int getRequestsPerWindow() { return requestsPerWindow; }
    public void setRequestsPerWindow(int requestsPerWindow) { this.requestsPerWindow = requestsPerWindow; }
    
    public int getWindowSizeMinutes() { return windowSizeMinutes; }
    public void setWindowSizeMinutes(int windowSizeMinutes) { this.windowSizeMinutes = windowSizeMinutes; }
    
    public int getMaxResultsPerRequest() { return maxResultsPerRequest; }
    public void setMaxResultsPerRequest(int maxResultsPerRequest) { this.maxResultsPerRequest = maxResultsPerRequest; }
    
    public int getDefaultTweetCount() { return defaultTweetCount; }
    public void setDefaultTweetCount(int defaultTweetCount) { this.defaultTweetCount = defaultTweetCount; }
    
    public String getDefaultTweetFields() { return defaultTweetFields; }
    public void setDefaultTweetFields(String defaultTweetFields) { this.defaultTweetFields = defaultTweetFields; }
    
    public String getDefaultUserFields() { return defaultUserFields; }
    public void setDefaultUserFields(String defaultUserFields) { this.defaultUserFields = defaultUserFields; }
    
    public String getDefaultExpansions() { return defaultExpansions; }
    public void setDefaultExpansions(String defaultExpansions) { this.defaultExpansions = defaultExpansions; }
    
    public boolean isExcludeRetweets() { return excludeRetweets; }
    public void setExcludeRetweets(boolean excludeRetweets) { this.excludeRetweets = excludeRetweets; }
    
    public boolean isExcludeReplies() { return excludeReplies; }
    public void setExcludeReplies(boolean excludeReplies) { this.excludeReplies = excludeReplies; }
    
    public String[] getDefaultLanguages() { return defaultLanguages; }
    public void setDefaultLanguages(String[] defaultLanguages) { this.defaultLanguages = defaultLanguages; }
}