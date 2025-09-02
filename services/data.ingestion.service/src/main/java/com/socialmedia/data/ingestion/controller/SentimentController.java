package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.repository.SentimentDataRepository;
import com.socialmedia.data.ingestion.repository.SocialPostRepository;
import com.socialmedia.data.ingestion.service.StanfordSentimentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Sentiment Analysis operations
 * Integrates with your existing AWS deployment and Redis caching
 */
@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
    
    private static final Logger logger = LoggerFactory.getLogger(SentimentController.class);
    
    @Autowired
    private StanfordSentimentService sentimentService;
    
    @Autowired
    private SentimentDataRepository sentimentDataRepository;
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    /**
     * Health check for sentiment analysis service
     * Integrates with your existing health check pattern
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> health = sentimentService.getHealthStatus();
            
            boolean isHealthy = "UP".equals(health.get("status"));
            return ResponseEntity
                .status(isHealthy ? 200 : 503)
                .body(health);
            
        } catch (Exception e) {
            logger.error("Sentiment service health check failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = Map.of(
                "service", "Stanford Sentiment Analysis",
                "status", "DOWN",
                "error", e.getMessage(),
                "lastCheck", LocalDateTime.now()
            );
            
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
    
/**
     * Manually trigger sentiment analysis for a specific post
     * Changed to GET request since we're not modifying data, just analyzing
     */
    @GetMapping("/analyze/{postId}")
    public ResponseEntity<Map<String, Object>> analyzePost(@PathVariable Long postId) {
        try {
            Optional<SocialPost> postOpt = socialPostRepository.findById(postId);
            
            if (postOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Post not found with ID: " + postId
                ));
            }
            
            SocialPost post = postOpt.get();
            
            // Check if already analyzed
            Optional<SentimentData> existing = sentimentDataRepository.findBySocialPostId(postId);
            
            SentimentData sentiment;
            if (existing.isPresent()) {
                // Return existing analysis instead of re-analyzing
                sentiment = existing.get();
                logger.info("Returning existing sentiment analysis for post: {}", postId);
            } else {
                // First-time analysis
                logger.info("Analyzing sentiment for post: {}", postId);
                sentiment = sentimentService.analyzeSentiment(post);
            }
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Sentiment analysis completed",
                "postId", postId,
                "sentiment", Map.of(
                    "label", sentiment.getSentimentLabel().name(),
                    "score", sentiment.getSentimentScore(),
                    "confidence", sentiment.getConfidence()
                ),
                "processedAt", sentiment.getProcessedAt()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to analyze sentiment for post {}: {}", postId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Sentiment analysis failed: " + e.getMessage(),
                "postId", postId
            ));
        }
    }    
    
    /**
     * Batch sentiment analysis for multiple posts
     * Async operation that returns immediately
     */
    @PostMapping("/analyze/batch")
    public ResponseEntity<Map<String, Object>> analyzeBatch(@RequestParam(defaultValue = "50") int limit) {
        try {
            // Find posts without sentiment analysis
            List<SocialPost> unanalyzedPosts = socialPostRepository
                .findPostsWithoutSentiment(limit);
            
            if (unanalyzedPosts.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "info",
                    "message", "No posts found requiring sentiment analysis",
                    "postsQueued", 0
                ));
            }
            
            // Trigger async batch processing
            CompletableFuture<List<SentimentData>> future = sentimentService.analyzeSentimentBatch(unanalyzedPosts);
            
            logger.info("Queued {} posts for batch sentiment analysis", unanalyzedPosts.size());
            
            return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "message", "Batch sentiment analysis started",
                "postsQueued", unanalyzedPosts.size(),
                "estimatedCompletionMinutes", Math.max(1, unanalyzedPosts.size() / 10) // Rough estimate
            ));
            
        } catch (Exception e) {
            logger.error("Failed to start batch sentiment analysis: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to start batch analysis: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get sentiment statistics for a platform
     * Integrates with your existing Redis caching
     */
    @GetMapping("/stats/{platform}")
    public ResponseEntity<Map<String, Object>> getPlatformSentimentStats(@PathVariable Platform platform) {
        try {
            Map<String, Object> stats = sentimentService.getSentimentStatistics(platform);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "platform", platform.name(),
                "statistics", stats,
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get sentiment statistics for {}: {}", platform, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to retrieve sentiment statistics",
                "platform", platform.name()
            ));
        }
    }
    
    /**
     * Get sentiment distribution across all platforms
     */
    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Object>> getSentimentDistribution() {
        try {
            // Get overall distribution from database
            List<Object[]> redditDist = sentimentDataRepository.getSentimentDistributionByPlatform(Platform.REDDIT);
            List<Object[]> youtubeDist = sentimentDataRepository.getSentimentDistributionByPlatform(Platform.YOUTUBE);
            
            Map<String, Object> distribution = Map.of(
                "reddit", redditDist,
                "youtube", youtubeDist,
                "generatedAt", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "distribution", distribution
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get sentiment distribution: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to retrieve sentiment distribution"
            ));
        }
    }
    
    /**
     * Get sentiment analysis progress/status
     */
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getAnalysisProgress() {
        try {
            // Count total posts vs analyzed posts
            long totalPosts = socialPostRepository.count();
            long analyzedPosts = sentimentDataRepository.count();
            long unanalyzedPosts = totalPosts - analyzedPosts;
            
            double completionPercentage = totalPosts > 0 ? ((double) analyzedPosts / totalPosts) * 100 : 0.0;
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "progress", Map.of(
                    "totalPosts", totalPosts,
                    "analyzedPosts", analyzedPosts,
                    "unanalyzedPosts", unanalyzedPosts,
                    "completionPercentage", Math.round(completionPercentage * 100.0) / 100.0
                ),
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to get analysis progress: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to retrieve analysis progress"
            ));
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSentiment(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Text parameter is required"
                ));
            }
            
            // Create and save a temporary post for testing
            SocialPost testPost = new SocialPost();
            testPost.setPlatform(Platform.REDDIT);
            testPost.setContent(text);
            testPost.setExternalId("test-" + System.currentTimeMillis());
            testPost.setTitle("Test Post");
            testPost.setAuthor("test-user");
            testPost.setCreatedAt(LocalDateTime.now());
            testPost.setIngestedAt(LocalDateTime.now());
            
            // SAVE THE POST FIRST
            SocialPost savedPost = socialPostRepository.save(testPost);
            
            // Then analyze sentiment
            SentimentData result = sentimentService.analyzeSentiment(savedPost);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test sentiment analysis completed",
                "input", text,
                "result", Map.of(
                    "label", result.getSentimentLabel().name(),
                    "score", result.getSentimentScore(),
                    "confidence", result.getConfidence(),
                    "interpretation", interpretSentiment(result.getSentimentLabel(), result.getSentimentScore())
                )
            ));
            
        } catch (Exception e) {
            logger.error("Test sentiment analysis failed: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Test analysis failed: " + e.getMessage()
            ));
        }
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Provide human-readable interpretation of sentiment results
     */
    private String interpretSentiment(SentimentLabel label, double score) {
        return switch (label) {
            case POSITIVE -> String.format("Positive sentiment (%.1f%% positive)", score * 100);
            case NEGATIVE -> String.format("Negative sentiment (%.1f%% negative)", (1 - score) * 100);
            case NEUTRAL -> "Neutral sentiment - neither clearly positive nor negative";
            case MIXED -> "Mixed sentiment - contains both positive and negative elements";
            case UNKNOWN -> "Unable to determine sentiment";
        };
    }
}