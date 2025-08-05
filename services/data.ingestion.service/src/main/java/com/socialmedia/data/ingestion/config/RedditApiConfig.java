package com.socialmedia.data.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;

@Configuration
@ConfigurationProperties(prefix = "reddit.api")
@Validated
public class RedditApiConfig {
    
    @NotBlank
    private String baseUrl;    
    @NotBlank
    private String userAgent = "SentimentAnalytics/1.0";
    
    @Positive
    private int requestsPerMinute = 60;
    
    @Positive
    private int connectionTimeoutMs = 5000;
    
    @Positive
    private int readTimeoutMs = 10000;
    
    @Positive
    private int maxRetries = 3;
    
    @Positive
    private long retryDelayMs = 1000;
    
    // Default subreddits to monitor
    private String[] defaultSubreddits = {
        "technology", "programming", "worldnews", "AskReddit"
    };
    
    // Getters and setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public int getRequestsPerMinute() { return requestsPerMinute; }
    public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
    
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    
    public String[] getDefaultSubreddits() { return defaultSubreddits; }
    public void setDefaultSubreddits(String[] defaultSubreddits) { this.defaultSubreddits = defaultSubreddits; }
    
    // Add this helper method for logging
    @Override
    public String toString() {
        return "RedditApiConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", requestsPerMinute=" + requestsPerMinute +
                ", defaultSubreddits=" + Arrays.toString(defaultSubreddits) +
                '}';
    }
}