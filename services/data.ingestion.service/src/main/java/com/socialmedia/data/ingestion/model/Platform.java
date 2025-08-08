package com.socialmedia.data.ingestion.model;

/**
 * Enumeration representing supported social media platforms
 * for the sentiment analytics system.
 */
public enum Platform {
    REDDIT("reddit", "Reddit"),
    TWITTER("twitter", "Twitter/X"),
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
     * @return Platform enum or null if not found
     */
    public static Platform fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (Platform platform : Platform.values()) {
            if (platform.value.equalsIgnoreCase(value)) {
                return platform;
            }
        }
        
        throw new IllegalArgumentException("Unknown platform: " + value);
    }
    
    /**
     * Check if platform supports specific features
     */
    public boolean supportsUpvotes() {
        return this == REDDIT;
    }
    
    public boolean supportsRetweets() {
        return this == TWITTER;
    }
    
    public boolean supportsVideoMetrics() {
        return this == YOUTUBE;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}