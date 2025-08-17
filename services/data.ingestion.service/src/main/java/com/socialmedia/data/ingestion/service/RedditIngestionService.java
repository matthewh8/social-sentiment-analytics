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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class RedditIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditIngestionService.class);
    
    // Session counter for tracking ingested posts
    private final AtomicInteger sessionIngestedCount = new AtomicInteger(0);
    
    @Autowired
    private RedditApiClient redditApiClient;
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    /**
     * 🎯 MAIN METHOD: Ingest posts from a single subreddit
     */
    public Mono<Integer> ingestFromSubreddit(String subreddit, int limit) {
        logger.info("Starting ingestion from r/{} with limit {}", subreddit, limit);
        
        return redditApiClient.fetchSubredditPosts(subreddit, limit, null)
            .collectList()
            .flatMap(redditPosts -> {
                logger.info("Fetched {} posts from r/{}", redditPosts.size(), subreddit);
                
                // Convert to SocialPost objects
                List<SocialPost> socialPosts = redditPosts.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Filter out duplicates using new method signature
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new posts (filtered {} duplicates)", 
                    newPosts.size(), socialPosts.size() - newPosts.size());
                
                // Save to database
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                
                // Update session counter
                sessionIngestedCount.addAndGet(savedPosts.size());
                
                logger.info("Successfully saved {} posts from r/{}", savedPosts.size(), subreddit);
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting from r/{}: {}", subreddit, error.getMessage()));
    }
    
    /**
     * 🔄 DATA CONVERSION: RedditPost → SocialPost
     */
    private SocialPost convertToSocialPost(RedditPost redditPost) {
        // Create using new constructor and Platform enum
        SocialPost socialPost = new SocialPost(
            Platform.REDDIT,
            redditPost.getId(),
            redditPost.getContent(),
            redditPost.getAuthor()
        );
        
        // Set additional fields
        socialPost.setTitle(redditPost.getTitle());
        socialPost.setUrl(redditPost.getUrl());
        socialPost.setUpvotes(redditPost.getScore() != null ? redditPost.getScore().longValue() : 0L);
        socialPost.setCommentCount(redditPost.getNumComments() != null ? redditPost.getNumComments().longValue() : 0L);
        socialPost.setSubreddit(redditPost.getSubreddit());
                
        // Convert Reddit timestamp (Unix epoch) to LocalDateTime
        if (redditPost.getCreatedUtc() != null) {
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(redditPost.getCreatedUtc().longValue()),
                ZoneId.systemDefault()
            );
            socialPost.setCreatedAt(createdAt);
        }
        
        // Calculate engagement score using new method
        socialPost.calculateEngagementScore();
        
        return socialPost;
    }
    
    /**
     * 🎯 BATCH PROCESSING: Multiple subreddits
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
                
                // Filter duplicates using new method signature
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new posts from batch ingestion", newPosts.size());
                
                // Save all
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionIngestedCount.addAndGet(savedPosts.size());
                
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error in batch ingestion: {}", error.getMessage()));
    }
    
    /**
     * 🧪 TESTING METHOD: Manual ingestion trigger
     */
    public Mono<Integer> testIngestion() {
        logger.info("Running test ingestion from r/programming");
        return ingestFromSubreddit("programming", 5);
    }
    
    // ===== ADDITIONAL METHODS FOR CONTROLLER SUPPORT =====
    
    /**
     * Manual ingestion trigger (used by REST controller)
     */
    public Mono<Integer> triggerManualIngestion(String[] subreddits, int postsPerSubreddit) {
        List<String> subredditList = Arrays.asList(subreddits);
        return ingestFromMultipleSubreddits(subredditList, postsPerSubreddit);
    }
    
    /**
     * Ingest trending posts from r/popular
     */
    public Mono<Integer> ingestTrendingPosts(int limit) {
        logger.info("Ingesting trending posts from r/popular, limit: {}", limit);
        return ingestFromSubreddit("popular", limit);
    }
    
    /**
     * Get ingestion statistics
     */
    public IngestionStats getIngestionStats() {
        Long totalPosts = socialPostRepository.count();
        
        // Use new method to count Reddit posts
        Long redditPosts = socialPostRepository.countByPlatformSince(
            Platform.REDDIT, 
            LocalDateTime.now().minusYears(10) // Get all Reddit posts
        );
        
        // Get recent posts (last 24 hours)
        Long recentPosts = socialPostRepository.countByPlatformSince(
            Platform.REDDIT, 
            LocalDateTime.now().minusHours(24)
        );
        
        return new IngestionStats(totalPosts, redditPosts, recentPosts, sessionIngestedCount.get());
    }
    
    /**
     * Statistics data class
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
        
        // Getters
        public Long getTotalPosts() { return totalPosts; }
        public Long getRedditPosts() { return redditPosts; }
        public Long getRecentPosts() { return recentPosts; }
        public Integer getSessionTotal() { return sessionTotal; }
    }
}