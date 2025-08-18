package com.socialmedia.data.ingestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient Configuration for external APIs
 * Configures WebClient beans for Reddit and Twitter APIs
 */
@Configuration
public class WebClientConfig {
    
    /**
     * WebClient for Reddit API
     */
    @Bean("redditWebClient")
    public WebClient redditWebClient(RedditApiConfig redditConfig) {
        return WebClient.builder()
                .baseUrl(redditConfig.getBaseUrl())
                .defaultHeader("User-Agent", redditConfig.getUserAgent())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
    }
    
    /**
     * WebClient for Twitter API
     */
    @Bean("twitterWebClient") 
    public WebClient twitterWebClient(TwitterApiConfig twitterConfig) {
        return WebClient.builder()
                .baseUrl(twitterConfig.getBaseUrl())
                .defaultHeader("User-Agent", twitterConfig.getUserAgent())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB buffer for Twitter
                .build();
    }
}