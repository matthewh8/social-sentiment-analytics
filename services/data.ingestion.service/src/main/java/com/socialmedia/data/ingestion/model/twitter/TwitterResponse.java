package com.socialmedia.data.ingestion.model.twitter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Twitter API v2 Response Wrapper
 * Handles the standard Twitter API response format
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitterResponse {
    
    @JsonProperty("data")
    private List<Tweet> data;
    
    @JsonProperty("includes")
    private Includes includes;
    
    @JsonProperty("meta")
    private Meta meta;
    
    @JsonProperty("errors")
    private List<TwitterError> errors;
    
    // Constructors
    public TwitterResponse() {}
    
    // Nested classes for API response structure
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Includes {
        @JsonProperty("users")
        private List<TwitterUser> users;
        
        // Getters and Setters
        public List<TwitterUser> getUsers() { return users; }
        public void setUsers(List<TwitterUser> users) { this.users = users; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwitterUser {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("username")
        private String username;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("result_count")
        private Integer resultCount;
        
        @JsonProperty("next_token")
        private String nextToken;
        
        @JsonProperty("previous_token")
        private String previousToken;
        
        @JsonProperty("newest_id")
        private String newestId;
        
        @JsonProperty("oldest_id")
        private String oldestId;
        
        // Getters and Setters
        public Integer getResultCount() { return resultCount; }
        public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
        
        public String getNextToken() { return nextToken; }
        public void setNextToken(String nextToken) { this.nextToken = nextToken; }
        
        public String getPreviousToken() { return previousToken; }
        public void setPreviousToken(String previousToken) { this.previousToken = previousToken; }
        
        public String getNewestId() { return newestId; }
        public void setNewestId(String newestId) { this.newestId = newestId; }
        
        public String getOldestId() { return oldestId; }
        public void setOldestId(String oldestId) { this.oldestId = oldestId; }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TwitterError {
        @JsonProperty("detail")
        private String detail;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("parameter")
        private String parameter;
        
        // Getters and Setters
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getParameter() { return parameter; }
        public void setParameter(String parameter) { this.parameter = parameter; }
    }
    
    // Helper methods
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public String getFirstErrorMessage() {
        if (hasErrors()) {
            return errors.get(0).getDetail();
        }
        return null;
    }
    
    public String getUsernameById(String userId) {
        if (includes != null && includes.getUsers() != null) {
            return includes.getUsers().stream()
                    .filter(user -> userId.equals(user.getId()))
                    .map(TwitterUser::getUsername)
                    .findFirst()
                    .orElse("unknown_user");
        }
        return "unknown_user";
    }
    
    // Getters and Setters
    public List<Tweet> getData() { return data; }
    public void setData(List<Tweet> data) { this.data = data; }
    
    public Includes getIncludes() { return includes; }
    public void setIncludes(Includes includes) { this.includes = includes; }
    
    public Meta getMeta() { return meta; }
    public void setMeta(Meta meta) { this.meta = meta; }
    
    public List<TwitterError> getErrors() { return errors; }
    public void setErrors(List<TwitterError> errors) { this.errors = errors; }
}