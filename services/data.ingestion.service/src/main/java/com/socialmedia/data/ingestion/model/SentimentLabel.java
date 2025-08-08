package com.socialmedia.data.ingestion.model;

/**
 * Enumeration representing sentiment analysis classification labels
 */
public enum SentimentLabel {
    POSITIVE("positive", "Positive", 1),
    NEGATIVE("negative", "Negative", -1),
    NEUTRAL("neutral", "Neutral", 0),
    MIXED("mixed", "Mixed", 0),
    UNKNOWN("unknown", "Unknown", 0);
    
    private final String value;
    private final String displayName;
    private final int numericValue;
    
    SentimentLabel(String value, String displayName, int numericValue) {
        this.value = value;
        this.displayName = displayName;
        this.numericValue = numericValue;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getNumericValue() {
        return numericValue;
    }
    
    /**
     * Convert string value to SentimentLabel enum
     */
    public static SentimentLabel fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        for (SentimentLabel label : SentimentLabel.values()) {
            if (label.value.equalsIgnoreCase(value) || label.name().equalsIgnoreCase(value)) {
                return label;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Convert numeric sentiment score to label
     */
    public static SentimentLabel fromScore(double score) {
        if (score > 0.1) return POSITIVE;
        if (score < -0.1) return NEGATIVE;
        return NEUTRAL;
    }
    
    /**
     * Check if sentiment is positive or negative (not neutral)
     */
    public boolean isOpinionated() {
        return this == POSITIVE || this == NEGATIVE;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}