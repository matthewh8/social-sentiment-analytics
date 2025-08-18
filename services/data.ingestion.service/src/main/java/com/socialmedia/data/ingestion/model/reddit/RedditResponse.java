package com.socialmedia.data.ingestion.model.reddit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Reddit API response structure (2025 version)
 * Complete response wrapper for Reddit API calls
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditResponse {
    
    @JsonProperty("kind")
    private String kind;
    
    @JsonProperty("data")
    private RedditData data;
    
    // Constructors
    public RedditResponse() {}
    
    // Getters and setters
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    
    public RedditData getData() { return data; }
    public void setData(RedditData data) { this.data = data; }
    
    /**
     * Reddit data container with children posts
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditData {
        
        @JsonProperty("children")
        private List<RedditChild> children;
        
        @JsonProperty("after")
        private String after;
        
        @JsonProperty("before")
        private String before;
        
        @JsonProperty("dist")
        private Integer dist;
        
        // Constructors
        public RedditData() {}
        
        // Getters and setters
        public List<RedditChild> getChildren() { return children; }
        public void setChildren(List<RedditChild> children) { this.children = children; }
        
        public String getAfter() { return after; }
        public void setAfter(String after) { this.after = after; }
        
        public String getBefore() { return before; }
        public void setBefore(String before) { this.before = before; }
        
        public Integer getDist() { return dist; }
        public void setDist(Integer dist) { this.dist = dist; }
    }
    
    /**
     * Reddit child wrapper containing individual posts
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditChild {
        
        @JsonProperty("kind")
        private String kind;
        
        @JsonProperty("data")
        private RedditPost data;
        
        // Constructors
        public RedditChild() {}
        
        // Getters and setters
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        
        public RedditPost getData() { return data; }
        public void setData(RedditPost data) { this.data = data; }
    }
    
    @Override
    public String toString() {
        return "RedditResponse{" +
                "kind='" + kind + '\'' +
                ", data=" + data +
                '}';
    }
}