package com.socialmedia.data.ingestion.model.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeVideo {
    
    @JsonProperty("id")
    private Object id; // Can be String (videos API) or Object (search API)
    
    @JsonProperty("snippet")
    private YouTubeSnippet snippet;
    
    @JsonProperty("statistics")
    private YouTubeStatistics statistics;
    
    // Constructors
    public YouTubeVideo() {}
    
    // Smart getter that handles both API response formats
    public String getId() {
        if (id instanceof String) {
            return (String) id; // Direct from videos API
        } else if (id instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> idMap = (java.util.Map<String, Object>) id;
            return (String) idMap.get("videoId"); // From search API
        }
        return null;
    }
    
    // Raw getter for internal processing
    public Object getIdObject() { return id; }
    
    public void setId(Object id) { this.id = id; }
    
    public YouTubeSnippet getSnippet() { return snippet; }
    public void setSnippet(YouTubeSnippet snippet) { this.snippet = snippet; }
    
    public YouTubeStatistics getStatistics() { return statistics; }
    public void setStatistics(YouTubeStatistics statistics) { this.statistics = statistics; }
    
    // Helper methods for conversion
    public String getTitle() {
        return snippet != null ? snippet.getTitle() : null;
    }
    
    public String getDescription() {
        return snippet != null ? snippet.getDescription() : null;
    }
    
    public String getChannelTitle() {
        return snippet != null ? snippet.getChannelTitle() : null;
    }
    
    public String getPublishedAt() {
        return snippet != null ? snippet.getPublishedAt() : null;
    }
    
    public Long getLikeCount() {
        return statistics != null ? statistics.getLikeCount() : 0L;
    }
    
    public Long getViewCount() {
        return statistics != null ? statistics.getViewCount() : 0L;
    }
    
    public Long getCommentCount() {
        return statistics != null ? statistics.getCommentCount() : 0L;
    }
}