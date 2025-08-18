package com.socialmedia.data.ingestion.model;

/**
 * Enumeration representing supported social media platforms
 * Supports Reddit and YouTube only (2025 version)
 */
public enum Platform {
    REDDIT("reddit", "Reddit"),
    YOUTUBE("youtube", "YouTube");
    
    private final String value;
    private final String displayName;
    
    Platform(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Convert string value to Platform enum
     * @param value the string value to convert
     * @return Platform enum or throw exception if not found
     */
    public static Platform fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Platform value cannot be null");
        }
        
        for (Platform platform : Platform.values()) {
            if (platform.value.equalsIgnoreCase(value)) {
                return platform;
            }
        }
        
        throw new IllegalArgumentException("Unsupported platform: " + value + 
            ". Supported platforms: reddit, youtube");
    }
    
    /**
     * Check if platform supports upvotes (Reddit-specific feature)
     */
    public boolean supportsUpvotes() {
        return this == REDDIT;
    }
    
    /**
     * Check if platform supports video metrics (YouTube-specific)
     */
    public boolean supportsVideoMetrics() {
        return this == YOUTUBE;
    }
    
    /**
     * Check if platform supports subreddit categorization
     */
    public boolean supportsSubreddits() {
        return this == REDDIT;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}