package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.model.twitter.Tweet;
import com.socialmedia.data.ingestion.repository.SocialPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Twitter ingestion service for MVP
 * Simplified for essential Twitter data ingestion and processing
 */
@Service
public class TwitterIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwitterIngestionService.class);
    
    private final AtomicInteger sessionCounter = new AtomicInteger(0);
    
    @Autowired
    private TwitterApiClient twitterApiClient;
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    // Default search queries for testing and general content
    private static final List<String> DEFAULT_QUERIES = List.of(
        "technology", "programming", "ai", "software development", "startup"
    );
    
    /**
     * Ingest tweets by search query
     */
    public Mono<Integer> ingestByQuery(String query, int limit) {
        logger.info("Starting Twitter ingestion for query: '{}' with limit {}", query, limit);
        
        return twitterApiClient.searchRecentTweets(query, limit)
            .collectList()
            .flatMap(tweets -> {
                logger.info("Fetched {} tweets for query '{}'", tweets.size(), query);
                
                // Convert to SocialPost entities
                List<SocialPost> socialPosts = tweets.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Simple duplicate filtering
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                        post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new tweets (filtered {} duplicates)", 
                    newPosts.size(), socialPosts.size() - newPosts.size());
                
                // Save all new posts
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                
                logger.info("Successfully saved {} tweets for query '{}'", savedPosts.size(), query);
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting tweets for query '{}': {}", query, error.getMessage()));
    }
    
    /**
     * Ingest tweets from multiple search queries
     */
    public Mono<Integer> ingestFromMultipleQueries(List<String> queries, int limitPerQuery) {
        logger.info("Starting batch Twitter ingestion from {} queries", queries.size());
        
        return twitterApiClient.searchMultipleQueries(queries, limitPerQuery)
            .collectList()
            .flatMap(allTweets -> {
                logger.info("Fetched {} total tweets from all queries", allTweets.size());
                
                // Convert all tweets
                List<SocialPost> socialPosts = allTweets.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Filter duplicates
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                        post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new tweets from batch ingestion", newPosts.size());
                
                // Save all
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error in batch Twitter ingestion: {}", error.getMessage()));
    }
    
    /**
     * Ingest tweets from a specific user
     */
    public Mono<Integer> ingestFromUser(String userId, int limit) {
        logger.info("Starting Twitter ingestion for user: {} with limit {}", userId, limit);
        
        return twitterApiClient.getUserTweets(userId, limit)
            .collectList()
            .flatMap(tweets -> {
                logger.info("Fetched {} tweets from user {}", tweets.size(), userId);
                
                // Convert to SocialPost entities
                List<SocialPost> socialPosts = tweets.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Filter duplicates
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                        post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new tweets from user {} (filtered {} duplicates)", 
                    newPosts.size(), userId, socialPosts.size() - newPosts.size());
                
                // Save all new posts
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                
                logger.info("Successfully saved {} tweets from user {}", savedPosts.size(), userId);
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting tweets from user {}: {}", userId, error.getMessage()));
    }
    
    /**
     * Simple test ingestion using default queries
     */
    public Mono<Integer> testIngestion() {
        logger.info("Running test Twitter ingestion with default queries");
        return ingestFromMultipleQueries(DEFAULT_QUERIES, 5);
    }
    
    /**
     * Get basic Twitter ingestion statistics
     */
    public IngestionStats getIngestionStats() {
        Long totalPosts = socialPostRepository.count();
        Long twitterPosts = socialPostRepository.countByPlatformSince(
            Platform.TWITTER, 
            LocalDateTime.now().minusYears(1) // Get all Twitter posts from last year
        );
        Long recentPosts = socialPostRepository.countByPlatformSince(
            Platform.TWITTER, 
            LocalDateTime.now().minusHours(24)
        );
        
        return new IngestionStats(totalPosts, twitterPosts, recentPosts, sessionCounter.get());
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Convert Tweet to SocialPost entity (simplified)
     */
    private SocialPost convertToSocialPost(Tweet tweet) {
        SocialPost socialPost = new SocialPost();
        
        // Basic fields
        socialPost.setPlatform(Platform.TWITTER);
        socialPost.setExternalId(tweet.getId());
        socialPost.setTitle(null); // Twitter doesn't have titles
        socialPost.setContent(tweet.getText());
        socialPost.setAuthor(extractUsername(tweet.getAuthorId())); // Simplified username extraction
        
        // Parse timestamp
        if (tweet.getCreatedAt() != null) {
            try {
                LocalDateTime createdAt = LocalDateTime.parse(
                    tweet.getCreatedAt().replace("Z", ""),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );
                socialPost.setCreatedAt(createdAt);
            } catch (DateTimeParseException e) {
                logger.warn("Failed to parse tweet timestamp: {}", tweet.getCreatedAt());
                socialPost.setCreatedAt(LocalDateTime.now());
            }
        }
        
        // Engagement metrics from public metrics
        if (tweet.getPublicMetrics() != null) {
            var metrics = tweet.getPublicMetrics();
            socialPost.setLikeCount(metrics.getLikeCount());
            socialPost.setShareCount(addNullSafe(metrics.getRetweetCount(), metrics.getQuoteCount()));
            socialPost.setCommentCount(metrics.getReplyCount());
            // Twitter doesn't have upvotes, keep null
            // viewCount could be impression_count but that's often not available for basic access
        }
        
        // Calculate engagement score
        socialPost.setEngagementScore(calculateTwitterEngagement(socialPost));
        
        return socialPost;
    }
    
    /**
     * Simple username extraction (for MVP, use author_id as username)
     */
    private String extractUsername(String authorId) {
        // In a full implementation, we'd look up the username from the includes section
        // For MVP, we'll use a simple format
        return "user_" + authorId;
    }
    
    /**
     * Helper method to safely add two potentially null Long values
     */
    private Long addNullSafe(Long a, Long b) {
        long result = 0;
        if (a != null) result += a;
        if (b != null) result += b;
        return result > 0 ? result : null;
    }
    
    /**
     * Calculate Twitter-specific engagement score
     */
    private double calculateTwitterEngagement(SocialPost post) {
        long likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        long shares = post.getShareCount() != null ? post.getShareCount() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Twitter engagement formula: likes + (shares * 3) + (comments * 2)
        double score = likes + (shares * 3) + (comments * 2);
        return Math.max(0, Math.min(100, score / 20)); // Normalize to 0-100
    }
    
    /**
     * Simple statistics data class (reusing from Reddit implementation)
     */
    public static class IngestionStats {
        private final Long totalPosts;
        private final Long twitterPosts;
        private final Long recentPosts;
        private final Integer sessionTotal;
        
        public IngestionStats(Long totalPosts, Long twitterPosts, Long recentPosts, Integer sessionTotal) {
            this.totalPosts = totalPosts;
            this.twitterPosts = twitterPosts;
            this.recentPosts = recentPosts;
            this.sessionTotal = sessionTotal;
        }
        
        public Long getTotalPosts() { return totalPosts; }
        public Long getTwitterPosts() { return twitterPosts; }
        public Long getRecentPosts() { return recentPosts; }
        public Integer getSessionTotal() { return sessionTotal; }
    }
}