package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.model.youtube.YouTubeVideo;
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
 * Simplified YouTube ingestion service for MVP (2025 version)
 * Mirrors RedditIngestionService patterns exactly
 * Focuses on core ingestion functionality
 */
@Service
public class YouTubeIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeIngestionService.class);
    
    private final AtomicInteger sessionCounter = new AtomicInteger(0);
    
    @Autowired
    private YouTubeApiClient youtubeApiClient;
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    /**
     * Ingest videos from a single channel
     */
    public Mono<Integer> ingestFromChannel(String channelId, int limit) {
        logger.info("Starting ingestion from channel {} with limit {}", channelId, limit);
        
        return youtubeApiClient.fetchChannelVideos(channelId, limit, null)
            .collectList()
            .flatMap(youtubeVideos -> {
                logger.info("Fetched {} videos from channel {}", youtubeVideos.size(), channelId);
                
                // Convert to SocialPost entities
                List<SocialPost> socialPosts = youtubeVideos.stream()
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
                
                logger.info("Successfully saved {} videos from channel {}", savedPosts.size(), channelId);
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting from channel {}: {}", channelId, error.getMessage()));
    }
    
    /**
     * Ingest videos by search query
     */
    public Mono<Integer> ingestFromSearch(String query, int limit) {
        logger.info("Starting ingestion from search query '{}' with limit {}", query, limit);
        
        return youtubeApiClient.searchVideos(query, limit, null)
            .collectList()
            .flatMap(youtubeVideos -> {
                logger.info("Fetched {} videos from search query '{}'", youtubeVideos.size(), query);
                
                // Convert to SocialPost entities
                List<SocialPost> socialPosts = youtubeVideos.stream()
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
                
                logger.info("Successfully saved {} videos from search query '{}'", savedPosts.size(), query);
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting from search '{}': {}", query, error.getMessage()));
    }
    
    /**
     * Batch processing: Multiple channels
     */
    public Mono<Integer> ingestFromMultipleChannels(List<String> channelIds, int limitPerChannel) {
        logger.info("Starting batch ingestion from {} channels", channelIds.size());
        
        return youtubeApiClient.fetchMultipleChannels(channelIds, limitPerChannel)
            .collectList()
            .flatMap(allYouTubeVideos -> {
                logger.info("Fetched {} total videos from all channels", allYouTubeVideos.size());
                
                // Convert all videos
                List<SocialPost> socialPosts = allYouTubeVideos.stream()
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
     * Manual ingestion trigger (for API endpoints)
     */
    public Mono<Integer> triggerManualIngestion(String[] channelIds, int videosPerChannel) {
        logger.info("Manual ingestion triggered for {} channels", channelIds.length);
        
        List<String> channelList = List.of(channelIds);
        return ingestFromMultipleChannels(channelList, videosPerChannel);
    }
    
    /**
     * Ingest trending videos
     */
    public Mono<Integer> ingestTrendingVideos(int limit) {
        logger.info("Ingesting trending videos, limit: {}", limit);
        return youtubeApiClient.fetchTrendingVideos(limit)
            .collectList()
            .flatMap(youtubeVideos -> {
                logger.info("Fetched {} trending videos", youtubeVideos.size());
                
                // Convert to SocialPost entities
                List<SocialPost> socialPosts = youtubeVideos.stream()
                    .map(this::convertToSocialPost)
                    .collect(Collectors.toList());
                
                // Simple duplicate filtering
                List<SocialPost> newPosts = socialPosts.stream()
                    .filter(post -> !socialPostRepository.existsByExternalIdAndPlatform(
                        post.getExternalId(), post.getPlatform()))
                    .collect(Collectors.toList());
                
                logger.info("Saving {} new trending posts", newPosts.size());
                
                // Save all new posts
                List<SocialPost> savedPosts = socialPostRepository.saveAll(newPosts);
                sessionCounter.addAndGet(savedPosts.size());
                
                return Mono.just(savedPosts.size());
            })
            .doOnError(error -> logger.error("Error ingesting trending videos: {}", error.getMessage()));
    }
    
    /**
     * Simple test ingestion
     */
    public Mono<Integer> testIngestion() {
        logger.info("Running test ingestion from search 'programming'");
        return ingestFromSearch("programming", 5);
    }
    
    /**
     * Get basic ingestion statistics
     */
    public IngestionStats getIngestionStats() {
        Long totalPosts = socialPostRepository.count();
        Long youtubePosts = socialPostRepository.countByPlatformSince(
            Platform.YOUTUBE, 
            LocalDateTime.now().minusYears(1) // Get all YouTube posts from last year
        );
        Long recentPosts = socialPostRepository.countByPlatformSince(
            Platform.YOUTUBE, 
            LocalDateTime.now().minusHours(24)
        );
        
        return new IngestionStats(totalPosts, youtubePosts, recentPosts, sessionCounter.get());
    }
    
    /**
     * Convert YouTubeVideo to SocialPost entity (2025 version)
     * Mirrors convertToSocialPost from Reddit exactly
     */
    private SocialPost convertToSocialPost(YouTubeVideo youtubeVideo) {
        // Basic validation
        if (youtubeVideo.getId() == null || youtubeVideo.getId().trim().isEmpty()) {
            logger.warn("Skipping YouTube video with null/empty ID");
            throw new IllegalArgumentException("YouTube video ID cannot be null or empty");
        }
        
        // Create using constructor (Platform, external ID, title, content, author)
        String title = youtubeVideo.getTitle();
        if (title == null || title.trim().isEmpty()) {
            logger.warn("YouTube video {} has no title, using fallback", youtubeVideo.getId());
            title = "[No Title]"; // Fallback for edge cases
        }
        
        String content = youtubeVideo.getDescription();
        if (content == null) {
            content = ""; // Empty content is acceptable for YouTube videos
        }
        
        String author = youtubeVideo.getChannelTitle();
        if (author == null || author.trim().isEmpty()) {
            author = "[Unknown Channel]"; // Fallback for missing channel info
        }
        
        SocialPost socialPost = new SocialPost(
            Platform.YOUTUBE,
            youtubeVideo.getId(),
            title,
            content,
            author
        );
        
        // Set additional YouTube-specific fields
        socialPost.setVideoId(youtubeVideo.getId());
        socialPost.setLikeCount(youtubeVideo.getLikeCount());
        socialPost.setViewCount(youtubeVideo.getViewCount());
        socialPost.setCommentCount(youtubeVideo.getCommentCount());
        
        // Build YouTube URL
        socialPost.setUrl("https://www.youtube.com/watch?v=" + youtubeVideo.getId());
                
        // Convert YouTube timestamp (ISO 8601) to LocalDateTime
        if (youtubeVideo.getPublishedAt() != null) {
            try {
                // YouTube uses ISO 8601 format: 2024-08-18T15:30:00Z
                LocalDateTime createdAt = LocalDateTime.parse(
                    youtubeVideo.getPublishedAt(),
                    DateTimeFormatter.ISO_DATE_TIME
                );
                socialPost.setCreatedAt(createdAt);
            } catch (DateTimeParseException e) {
                // Fallback to current time if timestamp parsing fails
                socialPost.setCreatedAt(LocalDateTime.now());
                logger.warn("YouTube video {} has invalid timestamp, using current time", youtubeVideo.getId());
            }
        } else {
            // Fallback to current time if timestamp is missing
            socialPost.setCreatedAt(LocalDateTime.now());
            logger.warn("YouTube video {} missing timestamp, using current time", youtubeVideo.getId());
        }
        
        // Calculate engagement score using the entity's method
        socialPost.calculateEngagementScore();
        
        return socialPost;
    }
    
    /**
     * Simple statistics data class (reuse same as Reddit)
     */
    public static class IngestionStats {
        private final Long totalPosts;
        private final Long youtubePosts;
        private final Long recentPosts;
        private final Integer sessionTotal;
        
        public IngestionStats(Long totalPosts, Long youtubePosts, Long recentPosts, Integer sessionTotal) {
            this.totalPosts = totalPosts;
            this.youtubePosts = youtubePosts;
            this.recentPosts = recentPosts;
            this.sessionTotal = sessionTotal;
        }
        
        public Long getTotalPosts() { return totalPosts; }
        public Long getYoutubePosts() { return youtubePosts; }
        public Long getRecentPosts() { return recentPosts; }
        public Integer getSessionTotal() { return sessionTotal; }
    }
}