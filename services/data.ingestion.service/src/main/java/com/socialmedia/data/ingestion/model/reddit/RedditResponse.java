package com.socialmedia.data.ingestion.model.reddit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditResponse {
    
    @JsonProperty("data")
    private RedditData data;
    
    public RedditData getData() { return data; }
    public void setData(RedditData data) { this.data = data; }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditData {
        
        @JsonProperty("children")
        private List<RedditChild> children;
        
        @JsonProperty("after")
        private String after;
        
        @JsonProperty("before")
        private String before;
        
        public List<RedditChild> getChildren() { return children; }
        public void setChildren(List<RedditChild> children) { this.children = children; }
        
        public String getAfter() { return after; }
        public void setAfter(String after) { this.after = after; }
        
        public String getBefore() { return before; }
        public void setBefore(String before) { this.before = before; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RedditChild {
        
        @JsonProperty("data")
        private RedditPost data;
        
        public RedditPost getData() { return data; }
        public void setData(RedditPost data) { this.data = data; }
    }
}
