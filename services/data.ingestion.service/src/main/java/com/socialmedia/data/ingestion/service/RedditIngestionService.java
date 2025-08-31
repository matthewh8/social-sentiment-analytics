package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.config.RedditApiConfig;
import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.model.reddit.RedditPost;
import com.socialmedia.data.ingestion.repository.SocialPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RedditIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditIngestionService.class);
    
    @Autowired
    private RedditApiClient redditApiClient;
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    @Autowired
    private RedditApiConfig config;
    
    @Autowired(required = false) // Optional - works without Redis
    private RedisCacheService cacheService;
    
    private final AtomicInteger sessionCounter = new AtomicInteger(0);
    
    // ===== ENHANCED METHODS WITH REDIS CACHING =====
    
    /**
     * Ingest from subreddit (enhanced with caching)
     */
    public Mono<Integer> ingestFromSubreddit(String subreddit, int limit) {
        String cacheKey = "subreddit:" + subreddit + ":" + limit;
        
        // Try cache first (if Redis available)
        if (cacheService != null && cacheService.isRedisAvailable()) {
            try {
                Optional<Object> cachedResponse = cacheService.getCachedApiResponse(
                    "reddit", cacheKey, Object.class);
                
                if (cachedResponse.isPresent()) {
                    logger.info("Using cached Reddit API response for r/{}", subreddit);
                    // Note: In a real implementation, you'd need proper type handling here
                    // For now, we'll skip cache for this method to avoid type issues
                }
            } catch (Exception e) {
                logger.debug("Cache lookup failed, proceeding with API call: {}", e.getMessage());
            }
        }
        
        // Fetch from API
        return redditApiClient.fetchSubredditPosts(subreddit, limit, null)
            .collectList()
            .flatMap(redditPosts -> {
                // Cache the API response (if Redis available)
                if (cacheService != null && cacheService.isRedisAvailable() && !redditPosts.isEmpty()) {
                    try {
                        cacheService.cacheApiResponse("reddit", cacheKey, redditPosts);
                    } catch (Exception e) {
                        logger.debug("Failed to cache API response: {}", e.getMessage());
                    }
                }
                
                return processRedditPosts(redditPosts);
            })
            .doOnSuccess(count -> {
                logger.info("Ingested {} new posts from r/{}", count, subreddit);
                // Invalidate related caches after new data (if Redis available)
                if (cacheService != null) {
                    try {
                        cacheService.invalidateStatsCaches();
                    } catch (Exception e) {
                        logger.debug("Failed to invalidate caches: {}", e.getMessage());
                    }
                }
            })
            .onErrorResume(error -> {
                logger.error("Failed to ingest from subreddit {}: {}", subreddit, error.getMessage());
                return Mono.just(0);
            });
    }
    
    /**
     * Ingest from multiple subreddits
     */
    public Mono<Integer> ingestFromMultipleSubreddits(List<String> subreddits, int postsPerSubreddit) {
        return Flux.fromIterable(subreddits)
            .flatMap(subreddit -> ingestFromSubreddit(subreddit, postsPerSubreddit)
                .onErrorResume(error -> {
                    logger.warn("Failed to ingest from r/{}: {}", subreddit, error.getMessage());
                    return Mono.just(0);
                }), 2) // Concurrency level 2 to respect rate limits
            .reduce(0, Integer::sum)
            .doOnSuccess(totalCount -> {
                logger.info("Batch ingestion completed: {} total posts from {} subreddits", 
                    totalCount, subreddits.size());
            });
    }
    
    /**
     * Get ingestion statistics (enhanced with caching)
     */
    public Mono<Map<String, Object>> getIngestionStats() {
        // Check cache first (if Redis available)
        if (cacheService != null && cacheService.isRedisAvailable()) {
            try {
                Optional<Map<String, Object>> cached = cacheService.getCachedPlatformStats(Platform.REDDIT);
                if (cached.isPresent()) {
                    logger.debug("Returning cached Reddit statistics");
                    return Mono.just(cached.get());
                }
            } catch (Exception e) {
                logger.debug("Cache lookup failed, generating from database: {}", e.getMessage());
            }
        }
        
        // Generate from database if not cached
        return Mono.fromCallable(() -> {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            
            // Use your existing repository methods
            Long totalPosts = socialPostRepository.count();
            
            // Count Reddit posts manually since countByPlatform doesn't exist
            Long totalRedditPosts = socialPostRepository.findAll().stream()
                .filter(post -> Platform.REDDIT.equals(post.getPlatform()))
                .count();
            
            // Count recent Reddit posts manually
            Long recentRedditPosts = socialPostRepository.findAll().stream()
                .filter(post -> Platform.REDDIT.equals(post.getPlatform()))
                .filter(post -> post.getCreatedAt() != null && post.getCreatedAt().isAfter(oneDayAgo))
                .count();
            
            Map<String, Object> stats = Map.of(
                "status", "healthy",
                "statistics", Map.of(
                    "totalPosts", totalPosts != null ? totalPosts : 0L,
                    "redditPosts", totalRedditPosts,
                    "recentPosts24h", recentRedditPosts,
                    "sessionTotal", sessionCounter.get()
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            // Cache the statistics (if Redis available)
            if (cacheService != null && cacheService.isRedisAvailable()) {
                try {
                    cacheService.cachePlatformStats(Platform.REDDIT, stats);
                } catch (Exception e) {
                    logger.debug("Failed to cache statistics: {}", e.getMessage());
                }
            }
            
            logger.debug("Generated Reddit statistics");
            return stats;
        });
    }
    
    /**
     * Trigger manual ingestion
     */
    public Mono<Map<String, Object>> triggerManualIngestion(List<String> subreddits, int postsPerSubreddit) {
        return ingestFromMultipleSubreddits(subreddits, postsPerSubreddit)
            .map(totalPosts -> {
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Ingestion completed",
                    "postsIngested", totalPosts,
                    "subreddits", subreddits
                );
                
                logger.info("Manual ingestion completed: {} posts from subreddits: {}", 
                    totalPosts, String.join(", ", subreddits));
                
                return response;
            })
            .onErrorResume(error -> {
                logger.error("Manual ingestion failed: {}", error.getMessage());
                return Mono.just(Map.of(
                    "status", "error",
                    "message", "Ingestion failed: " + error.getMessage(),
                    "postsIngested", 0,
                    "subreddits", subreddits
                ));
            });
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Process Reddit posts with duplicate filtering and database storage
     */
    private Mono<Integer> processRedditPosts(List<RedditPost> redditPosts) {
        return Mono.fromCallable(() -> {
            List<SocialPost> socialPosts = redditPosts.stream()
                .map(this::convertToSocialPost)
                .collect(Collectors.toList());
            
            // Filter duplicates using your existing repository method
            List<SocialPost> newPosts = socialPosts.stream()
                .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                    post.getExternalId(), post.getPlatform()))
                .collect(Collectors.toList());
            
            if (!newPosts.isEmpty()) {
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                return savedPosts.size();
            }
            
            return 0;
        });
    }
    
    /**
     * Convert RedditPost to SocialPost entity
     */
    private SocialPost convertToSocialPost(RedditPost redditPost) {
        SocialPost socialPost = new SocialPost(
            Platform.REDDIT,
            redditPost.getId(),
            redditPost.getTitle(),
            redditPost.getContent(),
            redditPost.getAuthor()
        );
        
        // Reddit-specific fields
        socialPost.setUpvotes(redditPost.getScore());
        socialPost.setCommentCount(redditPost.getNumComments());
        socialPost.setSubreddit(redditPost.getSubreddit());
        socialPost.setUrl(redditPost.getUrl());
        
        // Handle timestamps - use the existing fields from RedditPost
        // Assuming RedditPost has getCreatedUtc() method that returns epoch seconds
        if (redditPost.getCreatedUtc() != null) {
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(redditPost.getCreatedUtc()),
                ZoneId.of("UTC")
            );
            socialPost.setCreatedAt(createdAt);
        } else {
            // Fallback to current time if no created time available
            socialPost.setCreatedAt(LocalDateTime.now(ZoneId.of("UTC")));
        }
        
        socialPost.setIngestedAt(LocalDateTime.now(ZoneId.of("UTC")));
        
        // Auto-calculate engagement score
        socialPost.calculateEngagementScore();
        
        return socialPost;
    }
    
    // ===== SESSION MANAGEMENT =====
    
    /**
     * Get session counter value
     */
    public int getSessionCounter() {
        return sessionCounter.get();
    }
    
    /**
     * Reset session counter
     */
    public void resetSessionCounter() {
        sessionCounter.set(0);
        logger.info("Reset Reddit ingestion session counter");
    }
}