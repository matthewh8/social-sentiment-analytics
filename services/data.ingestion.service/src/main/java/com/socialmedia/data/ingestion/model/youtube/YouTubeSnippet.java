package com.socialmedia.data.ingestion.model.youtube;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeSnippet {
    
    @JsonProperty("publishedAt")
    private String publishedAt;
    
    @JsonProperty("channelId")
    private String channelId;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("channelTitle")
    private String channelTitle;
    
    @JsonProperty("categoryId")
    private String categoryId;
    
    @JsonProperty("liveBroadcastContent")
    private String liveBroadcastContent;
    
    @JsonProperty("defaultLanguage")
    private String defaultLanguage;
    
    // Constructors
    public YouTubeSnippet() {}
    
    // Getters and setters
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    
    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getChannelTitle() { return channelTitle; }
    public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }
    
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    
    public String getLiveBroadcastContent() { return liveBroadcastContent; }
    public void setLiveBroadcastContent(String liveBroadcastContent) { this.liveBroadcastContent = liveBroadcastContent; }
    
    public String getDefaultLanguage() { return defaultLanguage; }
    public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }
}
