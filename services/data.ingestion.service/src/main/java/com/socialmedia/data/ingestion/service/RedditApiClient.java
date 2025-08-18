package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.config.RedditApiConfig;
import com.socialmedia.data.ingestion.model.reddit.RedditPost;
import com.socialmedia.data.ingestion.model.reddit.RedditResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Simplified Reddit API Client for MVP
 * Removed: complex error handling, performance metrics, multiple endpoint support
 * Kept: basic API calls, rate limiting, simple error handling
 */
@Service
public class RedditApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditApiClient.class);
    
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    private final RedditApiConfig config;
    
    public RedditApiClient(WebClient redditWebClient, RateLimiter rateLimiter, RedditApiConfig config) {
        this.webClient = redditWebClient;
        this.rateLimiter = rateLimiter;
        this.config = config;
    }
    
    /**
     * Fetch posts from a subreddit (simplified)
     */
    public Flux<RedditPost> fetchSubredditPosts(String subreddit, int limit, String after) {
        logger.info("Fetching {} posts from r/{}", limit, subreddit);
        
        return rateLimiter.acquireToken()
            .then(makeApiCall(subreddit, limit, after))
            .retry(2) // Simple retry
            .flatMapMany(this::extractPosts)
            .doOnNext(post -> logger.debug("Fetched post: {} from r/{}", post.getId(), post.getSubreddit()))
            .doOnError(error -> logger.error("Error fetching posts from r/{}: {}", subreddit, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Returning empty flux due to error: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Fetch from multiple subreddits (simplified)
     */
    public Flux<RedditPost> fetchMultipleSubreddits(java.util.List<String> subreddits, int limitPerSubreddit) {
        logger.info("Fetching posts from {} subreddits", subreddits.size());
        
        return Flux.fromIterable(subreddits)
            .flatMap(subreddit -> fetchSubredditPosts(subreddit, limitPerSubreddit, null)
                .onErrorResume(error -> {
                    logger.warn("Failed to fetch from r/{}: {}", subreddit, error.getMessage());
                    return Flux.empty();
                }), 2) // Concurrency level of 2 (conservative)
            .doOnComplete(() -> logger.info("Completed fetching from all subreddits"));
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Make the actual API call to Reddit (simplified)
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
                logger.warn("Rate limited by Reddit API");
                return Mono.error(new RedditApiException("Rate limited"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from Reddit API: {}", response.statusCode());
                return Mono.error(new RedditApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from Reddit API: {}", response.statusCode());
                return Mono.error(new RedditApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(RedditResponse.class)
            .timeout(Duration.ofSeconds(30)); // Simple timeout
    }
    
    /**
     * Extract posts from Reddit API response (simplified)
     */
    private Flux<RedditPost> extractPosts(RedditResponse response) {
        if (response == null || response.getData() == null || response.getData().getChildren() == null) {
            logger.warn("Empty or invalid Reddit response");
            return Flux.empty();
        }
        
        return Flux.fromIterable(response.getData().getChildren())
            .map(child -> child.getData())
            .filter(this::isValidPost);
    }
    
    /**
     * Basic post validation
     */
    private boolean isValidPost(RedditPost post) {
        if (post == null) return false;
        
        // Filter out deleted/removed posts
        if (post.getAuthor() != null && post.getAuthor().equals("[deleted]")) {
            return false;
        }
        
        // Must have some content
        if ((post.getTitle() == null || post.getTitle().trim().isEmpty()) &&
            (post.getContent() == null || post.getContent().trim().isEmpty())) {
            return false;
        }
        
        // Skip NSFW for MVP
        if (Boolean.TRUE.equals(post.getOver18())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Simple exception for Reddit API errors
     */
    public static class RedditApiException extends RuntimeException {
        public RedditApiException(String message) {
            super(message);
        }
    }
}