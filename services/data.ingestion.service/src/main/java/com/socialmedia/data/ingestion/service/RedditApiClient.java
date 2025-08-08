// src/main/java/com/socialmedia/data/ingestion/service/RedditApiClient.java
package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.config.RedditApiConfig;
import com.socialmedia.data.ingestion.model.reddit.RedditPost;
import com.socialmedia.data.ingestion.model.reddit.RedditResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise Reddit API Client with comprehensive error handling,
 * rate limiting, and reactive patterns for high-throughput processing
 */
@Service
public class RedditApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditApiClient.class);
    
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    private final Retry retrySpec;
    private final RedditApiConfig config;
    
    public RedditApiClient(WebClient redditWebClient, 
                          RateLimiter rateLimiter, 
                          Retry redditApiRetrySpec,
                          RedditApiConfig config) {
        this.webClient = redditWebClient;
        this.rateLimiter = rateLimiter;
        this.retrySpec = redditApiRetrySpec;
        this.config = config;
    }
    
    /**
     * Fetch latest posts from a subreddit with rate limiting and error handling
     * 
     * @param subreddit The subreddit name (without r/)
     * @param limit Number of posts to fetch (max 100)
     * @param after Pagination token for fetching next page
     * @return Flux of RedditPost objects
     */
    public Flux<RedditPost> fetchSubredditPosts(String subreddit, int limit, String after) {
        logger.info("Fetching {} posts from r/{}", limit, subreddit);
        
        return rateLimiter.acquireToken()
            .then(makeApiCall(subreddit, limit, after))
            .retryWhen(retrySpec)
            .flatMapMany(this::extractPosts)
            .doOnNext(post -> logger.debug("Fetched post: {} from r/{}", post.getId(), post.getSubreddit()))
            .doOnError(error -> logger.error("Error fetching posts from r/{}: {}", subreddit, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Returning empty flux due to error: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Fetch posts from multiple subreddits concurrently
     */
    public Flux<RedditPost> fetchMultipleSubreddits(List<String> subreddits, int limitPerSubreddit) {
        logger.info("Fetching posts from {} subreddits: {}", subreddits.size(), subreddits);
        
        return Flux.fromIterable(subreddits)
            .flatMap(subreddit -> fetchSubredditPosts(subreddit, limitPerSubreddit, null)
                .onErrorResume(error -> {
                    logger.warn("Failed to fetch from r/{}, continuing with others: {}", subreddit, error.getMessage());
                    return Flux.empty();
                }), 3) // Concurrency level of 3 to respect rate limits
            .doOnComplete(() -> logger.info("Completed fetching from all subreddits"));
    }
    
    /**
     * Fetch trending posts from r/popular or r/all
     */
    public Flux<RedditPost> fetchTrendingPosts(String category, int limit) {
        String endpoint = switch (category.toLowerCase()) {
            case "popular" -> "/r/popular.json";
            case "all" -> "/r/all.json";
            default -> "/r/popular.json";
        };
        
        logger.info("Fetching {} trending posts from {}", limit, endpoint);
        
        return rateLimiter.acquireToken()
            .then(webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(endpoint)
                    .queryParam("limit", Math.min(limit, 100))
                    .queryParam("raw_json", 1)
                    .build())
                .retrieve()
                .bodyToMono(RedditResponse.class))
            .retryWhen(retrySpec)
            .flatMapMany(this::extractPosts)
            .doOnNext(post -> logger.debug("Fetched trending post: {}", post.getId()));
    }
    
    /**
     * Make the actual API call to Reddit
     */
    private Mono<RedditResponse> makeApiCall(String subreddit, int limit, String after) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/r/{subreddit}/new.json")
                    .queryParam("limit", Math.min(limit, 100))
                    .queryParam("raw_json", 1);
                
                if (after != null && !after.isEmpty()) {
                    builder.queryParam("after", after);
                }
                
                return builder.build(subreddit);
            })
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by Reddit API, will retry");
                return Mono.error(new RedditApiException("Rate limited"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from Reddit API: {}", response.statusCode());
                return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new RedditApiException("Client error: " + body)));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from Reddit API: {}", response.statusCode());
                return Mono.error(new RedditApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(RedditResponse.class)
            .doOnSuccess(response -> logger.debug("Successfully fetched Reddit API response"))
            .timeout(Duration.ofMillis(config.getReadTimeoutMs()));
    }
    
    /**
     * Extract posts from Reddit API response
     */
    private Flux<RedditPost> extractPosts(RedditResponse response) {
        if (response == null || response.getData() == null || response.getData().getChildren() == null) {
            logger.warn("Empty or invalid Reddit response");
            return Flux.empty();
        }
        
        return Flux.fromIterable(response.getData().getChildren())
            .map(child -> child.getData())
            .filter(this::isValidPost)
            .doOnNext(post -> logger.trace("Processing post: {} - {}", post.getId(), post.getTitle()));
    }
    
    /**
     * Validate post data quality
     */
    private boolean isValidPost(RedditPost post) {
        if (post == null) return false;
        
        // Filter out deleted/removed posts
        if (post.getAuthor() != null && post.getAuthor().equals("[deleted]")) {
            return false;
        }
        
        // Filter out posts without meaningful content
        if ((post.getTitle() == null || post.getTitle().trim().isEmpty()) &&
            (post.getContent() == null || post.getContent().trim().isEmpty())) {
            return false;
        }
        
        // Skip NSFW content for sentiment analysis
        if (Boolean.TRUE.equals(post.getOver18())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Custom exception for Reddit API errors
     */
    public static class RedditApiException extends RuntimeException {
        public RedditApiException(String message) {
            super(message);
        }
        
        public RedditApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}