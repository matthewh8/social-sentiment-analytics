package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.model.reddit.RedditPost;
import com.socialmedia.data.ingestion.repository.SocialPostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Simplified Reddit ingestion service for MVP
 * Removed: complex session tracking, advanced statistics, batch processing optimizations
 * Kept: core ingestion, basic conversion, simple duplicate filtering
 */
@Service
public class RedditIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditIngestionService.class);
    
    private final AtomicInteger sessionCounter = new AtomicInteger(0);
    
    @Autowired
    private RedditApiClient redditApiClient;
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    /**
     * Ingest posts from a single subreddit
     */
    public Mono<Integer> ingestFromSubreddit(String subreddit, int limit) {
        logger.info("Starting ingestion from r/{} with limit {}", subreddit, limit);
        
        return redditApiClient.fetchSubredditPosts(subreddit, limit, null)
            .collectList()
            .flatMap(redditPosts -> {
                logger.info("Fetched {} posts from r/{}", redditPosts.size(), subreddit);
                
                // Convert to SocialPost entities
                List<SocialPost> socialPosts = redditPosts.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Simple duplicate filtering
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                        post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new posts (filtered {} duplicates)", 
                    newPosts.size(), socialPosts.size() - newPosts.size());
                
                // Save all new posts
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                
                logger.info("Successfully saved {} posts from r/{}", savedPosts.size(), subreddit);
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting from r/{}: {}", subreddit, error.getMessage()));
    }
    
    /**
     * Ingest from multiple subreddits
     */
    public Mono<Integer> ingestFromMultipleSubreddits(List<String> subreddits, int limitPerSubreddit) {
        logger.info("Starting batch ingestion from {} subreddits", subreddits.size());
        
        return redditApiClient.fetchMultipleSubreddits(subreddits, limitPerSubreddit)
            .collectList()
            .flatMap(allRedditPosts -> {
                logger.info("Fetched {} total posts from all subreddits", allRedditPosts.size());
                
                // Convert all posts
                List<SocialPost> socialPosts = allRedditPosts.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Filter duplicates
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                        post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new posts from batch ingestion", newPosts.size());
                
                // Save all
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error in batch ingestion: {}", error.getMessage()));
    }
    
    /**
     * Simple test ingestion
     */
    public Mono<Integer> testIngestion() {
        logger.info("Running test ingestion from r/programming");
        return ingestFromSubreddit("programming", 5);
    }
    
    /**
     * Get basic ingestion statistics
     */
    public IngestionStats getIngestionStats() {
        Long totalPosts = socialPostRepository.count();
        Long redditPosts = socialPostRepository.countByPlatformSince(
            Platform.REDDIT, 
            LocalDateTime.now().minusYears(1) // Get all Reddit posts from last year
        );
        Long recentPosts = socialPostRepository.countByPlatformSince(
            Platform.REDDIT, 
            LocalDateTime.now().minusHours(24)
        );
        
        return new IngestionStats(totalPosts, redditPosts, recentPosts, sessionCounter.get());
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Convert RedditPost to SocialPost entity (simplified)
     */
    private SocialPost convertToSocialPost(RedditPost redditPost) {
        SocialPost socialPost = new SocialPost();
        
        // Basic fields
        socialPost.setPlatform(Platform.REDDIT);
        socialPost.setExternalId(redditPost.getId());
        socialPost.setTitle(redditPost.getTitle());
        socialPost.setContent(redditPost.getContent());
        socialPost.setAuthor(redditPost.getAuthor());
        socialPost.setSubreddit(redditPost.getSubreddit());
        
        // Engagement metrics
        socialPost.setUpvotes(redditPost.getScore() != null ? redditPost.getScore().longValue() : 0L);
        socialPost.setCommentCount(redditPost.getNumComments() != null ? redditPost.getNumComments().longValue() : 0L);
        
        // Convert timestamp
        if (redditPost.getCreatedUtc() != null) {
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(redditPost.getCreatedUtc()),
                ZoneId.systemDefault()
            );
            socialPost.setCreatedAt(createdAt);
        }
        
        // Calculate simple engagement score
        socialPost.setEngagementScore(calculateSimpleEngagement(socialPost));
        
        return socialPost;
    }
    
    /**
     * Simple engagement score calculation
     */
    private double calculateSimpleEngagement(SocialPost post) {
        long upvotes = post.getUpvotes() != null ? post.getUpvotes() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Simple formula: upvotes + (comments * 2), normalized to 0-100
        double score = upvotes + (comments * 2);
        return Math.max(0, Math.min(100, score / 10));
    }
    
    /**
     * Simple statistics data class
     */
    public static class IngestionStats {
        private final Long totalPosts;
        private final Long redditPosts;
        private final Long recentPosts;
        private final Integer sessionTotal;
        
        public IngestionStats(Long totalPosts, Long redditPosts, Long recentPosts, Integer sessionTotal) {
            this.totalPosts = totalPosts;
            this.redditPosts = redditPosts;
            this.recentPosts = recentPosts;
            this.sessionTotal = sessionTotal;
        }
        
        public Long getTotalPosts() { return totalPosts; }
        public Long getRedditPosts() { return redditPosts; }
        public Long getRecentPosts() { return recentPosts; }
        public Integer getSessionTotal() { return sessionTotal; }
    }
}