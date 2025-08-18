package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.service.RedditIngestionService;
import com.socialmedia.data.ingestion.service.RedditIngestionService.IngestionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified Reddit Controller for MVP
 * Removed: Complex configuration endpoints, detailed error handling
 * Kept: Essential ingestion triggers and health monitoring
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
        
        logger.info("Manual ingestion triggered for subreddits: {}", subreddits);
        
        List<String> subredditList = List.of(subreddits.split(","));
        
        return ingestionService.ingestFromMultipleSubreddits(subredditList, postsPerSubreddit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Ingestion completed");
                response.put("postsIngested", count);
                response.put("subreddits", subredditList);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Ingestion failed: {}", error.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Ingestion failed: " + error.getMessage());
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Test ingestion with default subreddits
     */
    @PostMapping("/test")
    public Mono<ResponseEntity<Map<String, Object>>> testIngestion() {
        logger.info("Test ingestion triggered");
        
        return ingestionService.testIngestion()
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Test ingestion completed");
                response.put("postsIngested", count);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Test ingestion failed: {}", error.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Test ingestion failed");
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Get basic ingestion statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.debug("Stats endpoint accessed");
        
        try {
            IngestionStats stats = ingestionService.getIngestionStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("totalPosts", stats.getTotalPosts());
            response.put("redditPosts", stats.getRedditPosts());
            response.put("recentPosts24h", stats.getRecentPosts());
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
     * Simple health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Reddit Ingestion Service");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
}