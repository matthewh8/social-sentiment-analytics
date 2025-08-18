package com.socialmedia.data.ingestion.model.reddit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditPost {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("selftext")
    private String content;
    
    @JsonProperty("author")
    private String author;
    
    @JsonProperty("subreddit")
    private String subreddit;
    
    @JsonProperty("created_utc")
    private Long createdUtc;
    
    @JsonProperty("score")
    private Long score;
    
    @JsonProperty("num_comments")
    private Long numComments;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("permalink")
    private String permalink;
    
    @JsonProperty("is_self")
    private Boolean isSelf;
    
    @JsonProperty("over_18")
    private Boolean over18;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getSubreddit() { return subreddit; }
    public void setSubreddit(String subreddit) { this.subreddit = subreddit; }
    
    public Long getCreatedUtc() { return createdUtc; }
    public void setCreatedUtc(Long createdUtc) { this.createdUtc = createdUtc; }
    
    public Long getScore() { return score; }
    public void setScore(Long score) { this.score = score; }
    
    public Long getNumComments() { return numComments; }
    public void setNumComments(Long numComments) { this.numComments = numComments; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getPermalink() { return permalink; }
    public void setPermalink(String permalink) { this.permalink = permalink; }
    
    public Boolean getIsSelf() { return isSelf; }
    public void setIsSelf(Boolean isSelf) { this.isSelf = isSelf; }
    
    public Boolean getOver18() { return over18; }
    public void setOver18(Boolean over18) { this.over18 = over18; }
}