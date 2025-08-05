// src/main/java/com/socialmedia/data/ingestion/config/ConfigurationLogger.java
package com.socialmedia.data.ingestion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationLogger.class);
    
    @Autowired
    private RedditApiConfig redditApiConfig;
    
    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        logger.info("=== Reddit Ingestion Service Configuration ===");
        logger.info("Reddit Ingestion Service initialized with subreddits: {}", 
                   java.util.Arrays.toString(redditApiConfig.getDefaultSubreddits()));
        logger.info("Reddit API Config loaded: baseUrl={}, requestsPerMinute={}", 
                   redditApiConfig.getBaseUrl(), redditApiConfig.getRequestsPerMinute());
        logger.info("Connection timeout: {}ms, Read timeout: {}ms", 
                   redditApiConfig.getConnectionTimeoutMs(), redditApiConfig.getReadTimeoutMs());
        logger.info("Max retries: {}, Retry delay: {}ms", 
                   redditApiConfig.getMaxRetries(), redditApiConfig.getRetryDelayMs());
        logger.info("User Agent: {}", redditApiConfig.getUserAgent());
        logger.info("=== Configuration Complete ===");
    }
}