// src/main/java/com/socialmedia/data/ingestion/controller/RedditController.java
package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.service.RedditIngestionService;
import com.socialmedia.data.ingestion.service.RedditIngestionService.IngestionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Reddit data ingestion operations
 * Provides endpoints for manual triggering, monitoring, and statistics
 */
@RestController
@RequestMapping("/api/reddit")
public class RedditController {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditController.class);
    
    private final RedditIngestionService ingestionService;
    
    public RedditController(RedditIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }
    
    /**
     * Trigger manual ingestion for specific subreddits
     */
    @PostMapping("/ingest")
    public Mono<ResponseEntity<Map<String, Object>>> triggerIngestion(
            @RequestParam(defaultValue = "technology,programming") String subreddits,
            @RequestParam(defaultValue = "25") int postsPerSubreddit) {
        
        logger.info("Manual ingestion triggered via API for subreddits: {}", subreddits);
        
        String[] subredditArray = subreddits.split(",");
        
        return ingestionService.triggerManualIngestion(subredditArray, postsPerSubreddit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Ingestion completed");
                response.put("postsIngested", count);
                response.put("subreddits", subredditArray);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Ingestion failed");
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Ingest trending posts from r/popular
     */
    @PostMapping("/trending")
    public Mono<ResponseEntity<Map<String, Object>>> ingestTrending(
            @RequestParam(defaultValue = "50") int limit) {
        
        logger.info("Trending posts ingestion triggered via API, limit: {}", limit);
        
        return ingestionService.ingestTrendingPosts(limit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Trending posts ingestion completed");
                response.put("postsIngested", count);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Trending ingestion failed");
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Get ingestion statistics and health status
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.debug("Stats endpoint accessed");
        
        try {
            IngestionStats stats = ingestionService.getIngestionStats();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPosts", stats.getTotalPosts());
            statistics.put("redditPosts", stats.getRedditPosts());
            statistics.put("recentPosts24h", stats.getRecentPosts());
            statistics.put("sessionTotal", stats.getSessionTotal());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("statistics", statistics);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving stats: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve statistics");
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Reddit Data Ingestion");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get service configuration info
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        // Note: In production, be careful about exposing sensitive config
        Map<String, Object> response = new HashMap<>();
        response.put("schedulingEnabled", true);
        response.put("schedulingInterval", "5 minutes");
        response.put("rateLimitEnabled", true);
        response.put("retryEnabled", true);
        response.put("maxRetries", 3);
        
        return ResponseEntity.ok(response);
    }
}