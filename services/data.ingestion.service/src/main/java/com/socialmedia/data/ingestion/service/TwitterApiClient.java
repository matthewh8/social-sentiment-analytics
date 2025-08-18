package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.config.TwitterApiConfig;
import com.socialmedia.data.ingestion.model.twitter.Tweet;
import com.socialmedia.data.ingestion.model.twitter.TwitterResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Twitter API Client for MVP
 * Simplified for essential tweet fetching with rate limiting
 */
@Service
public class TwitterApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(TwitterApiClient.class);
    
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    private final TwitterApiConfig config;
    
    public TwitterApiClient(WebClient twitterWebClient, RateLimiter rateLimiter, TwitterApiConfig config) {
        this.webClient = twitterWebClient;
        this.rateLimiter = rateLimiter;
        this.config = config;
        
        // Validate configuration on startup
        config.validateConfiguration();
        logger.info("Twitter API Client initialized with base URL: {}", config.getBaseUrl());
    }
    
    /**
     * Search recent tweets by query
     */
    public Flux<Tweet> searchRecentTweets(String query, int maxResults) {
        logger.info("Searching recent tweets for query: '{}', max results: {}", query, maxResults);
        
        return rateLimiter.acquireToken()
            .then(makeSearchApiCall(query, maxResults, null))
            .retry(2) // Simple retry for network issues
            .flatMapMany(this::extractTweets)
            .doOnNext(tweet -> logger.debug("Fetched tweet: {} by @{}", tweet.getId(), tweet.getAuthorId()))
            .doOnError(error -> logger.error("Error searching tweets for query '{}': {}", query, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Returning empty flux due to search error: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Search tweets from multiple queries
     */
    public Flux<Tweet> searchMultipleQueries(List<String> queries, int maxResultsPerQuery) {
        logger.info("Searching tweets from {} queries", queries.size());
        
        return Flux.fromIterable(queries)
            .flatMap(query -> searchRecentTweets(query, maxResultsPerQuery)
                .onErrorResume(error -> {
                    logger.warn("Failed to search for query '{}': {}", query, error.getMessage());
                    return Flux.empty();
                }), 2) // Conservative concurrency to respect rate limits
            .doOnComplete(() -> logger.info("Completed searching from all queries"));
    }
    
    /**
     * Get tweets by user ID
     */
    public Flux<Tweet> getUserTweets(String userId, int maxResults) {
        logger.info("Fetching tweets for user ID: {}, max results: {}", userId, maxResults);
        
        return rateLimiter.acquireToken()
            .then(makeUserTweetsApiCall(userId, maxResults, null))
            .retry(2)
            .flatMapMany(this::extractTweets)
            .doOnNext(tweet -> logger.debug("Fetched user tweet: {} by user {}", tweet.getId(), userId))
            .doOnError(error -> logger.error("Error fetching tweets for user {}: {}", userId, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Returning empty flux due to user tweets error: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Make search API call to Twitter
     */
    private Mono<TwitterResponse> makeSearchApiCall(String query, int maxResults, String nextToken) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/tweets/search/recent")
                    .queryParam("query", buildSearchQuery(query))
                    .queryParam("max_results", Math.min(maxResults, config.getMaxResultsPerRequest()))
                    .queryParam("tweet.fields", config.getDefaultTweetFields())
                    .queryParam("user.fields", config.getDefaultUserFields())
                    .queryParam("expansions", config.getDefaultExpansions());
                
                if (nextToken != null && !nextToken.isEmpty()) {
                    builder.queryParam("next_token", nextToken);
                }
                
                return builder.build();
            })
            .header("Authorization", config.getAuthorizationHeader())
            .header("User-Agent", config.getUserAgent())
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by Twitter API");
                return Mono.error(new TwitterApiException("Rate limited"));
            })
            .onStatus(status -> status == HttpStatus.UNAUTHORIZED, response -> {
                logger.error("Unauthorized - check Twitter Bearer Token");
                return Mono.error(new TwitterApiException("Unauthorized - invalid Bearer Token"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from Twitter API: {}", response.statusCode());
                return Mono.error(new TwitterApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from Twitter API: {}", response.statusCode());
                return Mono.error(new TwitterApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(TwitterResponse.class)
            .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Make user tweets API call
     */
    private Mono<TwitterResponse> makeUserTweetsApiCall(String userId, int maxResults, String paginationToken) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/users/{id}/tweets")
                    .queryParam("max_results", Math.min(maxResults, config.getMaxResultsPerRequest()))
                    .queryParam("tweet.fields", config.getDefaultTweetFields())
                    .queryParam("exclude", buildExclusionFilters());
                
                if (paginationToken != null && !paginationToken.isEmpty()) {
                    builder.queryParam("pagination_token", paginationToken);
                }
                
                return builder.build(userId);
            })
            .header("Authorization", config.getAuthorizationHeader())
            .header("User-Agent", config.getUserAgent())
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by Twitter API");
                return Mono.error(new TwitterApiException("Rate limited"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from Twitter API: {}", response.statusCode());
                return Mono.error(new TwitterApiException("Client error: " + response.statusCode()));
            })
            .bodyToMono(TwitterResponse.class)
            .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Extract tweets from API response
     */
    private Flux<Tweet> extractTweets(TwitterResponse response) {
        if (response == null) {
            logger.warn("Null Twitter response");
            return Flux.empty();
        }
        
        if (response.hasErrors()) {
            logger.warn("Twitter API returned errors: {}", response.getFirstErrorMessage());
            return Flux.empty();
        }
        
        if (!response.hasData()) {
            logger.debug("No tweets in response");
            return Flux.empty();
        }
        
        return Flux.fromIterable(response.getData())
            .filter(this::isValidTweet)
            .doOnNext(tweet -> enrichTweetWithUsername(tweet, response));
    }
    
    /**
     * Basic tweet validation for analytics
     */
    private boolean isValidTweet(Tweet tweet) {
        if (tweet == null || !tweet.isValidTweet()) {
            return false;
        }
        
        // Filter out deleted tweets or tweets with minimal content
        if (tweet.getText() == null || tweet.getText().trim().length() < 3) {
            return false;
        }
        
        // Basic language filtering (if configured)
        if (config.getDefaultLanguages().length > 0 && tweet.getLanguage() != null) {
            boolean languageMatches = false;
            for (String lang : config.getDefaultLanguages()) {
                if (lang.equals(tweet.getLanguage())) {
                    languageMatches = true;
                    break;
                }
            }
            if (!languageMatches) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Enrich tweet with username from includes section
     */
    private void enrichTweetWithUsername(Tweet tweet, TwitterResponse response) {
        if (response.getIncludes() != null) {
            String username = response.getUsernameById(tweet.getAuthorId());
            // Store username in a way that can be accessed later
            // For now, we'll use the authorId field to store username if needed
            // This is a simple approach for MVP
        }
    }
    
    /**
     * Build search query with filters
     */
    private String buildSearchQuery(String baseQuery) {
        StringBuilder query = new StringBuilder(baseQuery);
        
        if (config.isExcludeRetweets()) {
            query.append(" -is:retweet");
        }
        
        if (config.isExcludeReplies()) {
            query.append(" -is:reply");
        }
        
        // Add language filter if configured
        if (config.getDefaultLanguages().length > 0) {
            query.append(" lang:");
            query.append(config.getDefaultLanguages()[0]); // Use first language for simplicity
        }
        
        return query.toString();
    }
    
    /**
     * Build exclusion filters for user tweets
     */
    private String buildExclusionFilters() {
        StringBuilder exclusions = new StringBuilder();
        
        if (config.isExcludeRetweets()) {
            exclusions.append("retweets");
        }
        
        if (config.isExcludeReplies()) {
            if (exclusions.length() > 0) exclusions.append(",");
            exclusions.append("replies");
        }
        
        return exclusions.toString();
    }
    
    /**
     * Simple exception for Twitter API errors
     */
    public static class TwitterApiException extends RuntimeException {
        public TwitterApiException(String message) {
            super(message);
        }
        
        public TwitterApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}