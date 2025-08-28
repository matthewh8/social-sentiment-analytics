package com.socialmedia.data.ingestion.model.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * YouTube Data API v3 response structure (2025 version)
 * Complete response wrapper for YouTube API calls
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeResponse {
    
    @JsonProperty("kind")
    private String kind;
    
    @JsonProperty("etag")
    private String etag;
    
    @JsonProperty("nextPageToken")
    private String nextPageToken;
    
    @JsonProperty("prevPageToken")
    private String prevPageToken;
    
    @JsonProperty("pageInfo")
    private PageInfo pageInfo;
    
    @JsonProperty("items")
    private List<YouTubeVideo> items;
    
    // Constructors
    public YouTubeResponse() {}
    
    // Getters and setters
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    
    public String getEtag() { return etag; }
    public void setEtag(String etag) { this.etag = etag; }
    
    public String getNextPageToken() { return nextPageToken; }
    public void setNextPageToken(String nextPageToken) { this.nextPageToken = nextPageToken; }
    
    public String getPrevPageToken() { return prevPageToken; }
    public void setPrevPageToken(String prevPageToken) { this.prevPageToken = prevPageToken; }
    
    public PageInfo getPageInfo() { return pageInfo; }
    public void setPageInfo(PageInfo pageInfo) { this.pageInfo = pageInfo; }
    
    public List<YouTubeVideo> getItems() { return items; }
    public void setItems(List<YouTubeVideo> items) { this.items = items; }
    
    /**
     * YouTube page info for pagination
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageInfo {
        
        @JsonProperty("totalResults")
        private Integer totalResults;
        
        @JsonProperty("resultsPerPage")
        private Integer resultsPerPage;
        
        // Constructors
        public PageInfo() {}
        
        // Getters and setters
        public Integer getTotalResults() { return totalResults; }
        public void setTotalResults(Integer totalResults) { this.totalResults = totalResults; }
        
        public Integer getResultsPerPage() { return resultsPerPage; }
        public void setResultsPerPage(Integer resultsPerPage) { this.resultsPerPage = resultsPerPage; }
    }
    
    @Override
    public String toString() {
        return "YouTubeResponse{" +
                "kind='" + kind + '\'' +
                ", items=" + (items != null ? items.size() : 0) + " videos" +
                '}';
    }
}
