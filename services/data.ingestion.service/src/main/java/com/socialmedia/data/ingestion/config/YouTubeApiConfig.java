package com.socialmedia.data.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;

@ConfigurationProperties(prefix = "youtube.api")
@Validated
public class YouTubeApiConfig {
    
    @NotBlank
    private String baseUrl = "https://www.googleapis.com/youtube/v3";
    
    @NotBlank
    private String apiKey;
    
    @NotBlank
    private String userAgent = "SentimentAnalytics/1.0";
    
    @Positive
    private int requestsPerSecond = 100; // YouTube allows 100 requests/second
    
    @Positive
    private int quotaUnitsPerDay = 10000; // YouTube quota limit
    
    @Positive
    private int connectionTimeoutMs = 5000;
    
    @Positive
    private int readTimeoutMs = 10000;
    
    @Positive
    private int maxRetries = 3;
    
    @Positive
    private long retryDelayMs = 1000;
    
    // Default channels to monitor (using channel IDs)
    private String[] defaultChannels = {
        "UCBJycsmduvYEL83R_U4JriQ", // Marques Brownlee (MKBHD)
        "UCXuqSBlHAE6Xw-yeJA0Tunw", // Linus Tech Tips
        "UC-lHJZR3Gqxm24_Vd_AJ5Yw", // PewDiePie
        "UCsooa4yRKGN_zEE8iknghZA"  // TED
    };
    
    // Default search terms for trending content
    private String[] defaultSearchTerms = {
        "technology", "programming", "artificial intelligence", "software development"
    };
    
    // Video categories for trending (YouTube category IDs)
    private String[] defaultCategories = {
        "28", // Science & Technology
        "22", // People & Blogs
        "25", // News & Politics
        "27"  // Education
    };
    
    // Getters and setters
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public int getRequestsPerSecond() { return requestsPerSecond; }
    public void setRequestsPerSecond(int requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
    
    public int getQuotaUnitsPerDay() { return quotaUnitsPerDay; }
    public void setQuotaUnitsPerDay(int quotaUnitsPerDay) { this.quotaUnitsPerDay = quotaUnitsPerDay; }
    
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(int connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
    
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    
    public String[] getDefaultChannels() { return defaultChannels; }
    public void setDefaultChannels(String[] defaultChannels) { this.defaultChannels = defaultChannels; }
    
    public String[] getDefaultSearchTerms() { return defaultSearchTerms; }
    public void setDefaultSearchTerms(String[] defaultSearchTerms) { this.defaultSearchTerms = defaultSearchTerms; }
    
    public String[] getDefaultCategories() { return defaultCategories; }
    public void setDefaultCategories(String[] defaultCategories) { this.defaultCategories = defaultCategories; }
    
    // Add this helper method for logging
    @Override
    public String toString() {
        return "YouTubeApiConfig{" +
                "baseUrl='" + baseUrl + '\'' +
                ", requestsPerSecond=" + requestsPerSecond +
                ", quotaUnitsPerDay=" + quotaUnitsPerDay +
                ", defaultChannels=" + Arrays.toString(defaultChannels) +
                ", defaultSearchTerms=" + Arrays.toString(defaultSearchTerms) +
                '}';
    }
}