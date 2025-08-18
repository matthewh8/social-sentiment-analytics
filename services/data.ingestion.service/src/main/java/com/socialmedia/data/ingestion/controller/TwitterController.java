package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.service.TwitterIngestionService;
import com.socialmedia.data.ingestion.service.TwitterIngestionService.IngestionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Twitter Controller for MVP
 * Provides REST endpoints for Twitter data ingestion and monitoring
 */
@RestController
@RequestMapping("/api/twitter")
public class TwitterController {
    
    private static final Logger logger = LoggerFactory.getLogger(TwitterController.class);
    
    private final TwitterIngestionService ingestionService;
    
    public TwitterController(TwitterIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }
    
    /**
     * Trigger manual ingestion by search queries
     */
    @PostMapping("/ingest/search")
    public Mono<ResponseEntity<Map<String, Object>>> triggerSearchIngestion(
            @RequestParam(defaultValue = "technology,programming,ai") String queries,
            @RequestParam(defaultValue = "10") int tweetsPerQuery) {
        
        logger.info("Manual Twitter search ingestion triggered for queries: {}", queries);
        
        List<String> queryList = List.of(queries.split(","));
        
        return ingestionService.ingestFromMultipleQueries(queryList, tweetsPerQuery)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Twitter search ingestion completed");
                response.put("tweetsIngested", count);
                response.put("queries", queryList);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Twitter search ingestion failed: {}", error.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Twitter search ingestion failed: " + error.getMessage());
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Trigger ingestion by single search query
     */
    @PostMapping("/ingest/query")
    public Mono<ResponseEntity<Map<String, Object>>> triggerQueryIngestion(
            @RequestParam String query,
            @RequestParam(defaultValue = "25") int limit) {
        
        logger.info("Manual Twitter query ingestion triggered for: '{}'", query);
        
        return ingestionService.ingestByQuery(query, limit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Twitter query ingestion completed");
                response.put("tweetsIngested", count);
                response.put("query", query);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Twitter query ingestion failed for '{}': {}", query, error.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Twitter query ingestion failed: " + error.getMessage());
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Trigger ingestion from specific user
     */
    @PostMapping("/ingest/user")
    public Mono<ResponseEntity<Map<String, Object>>> triggerUserIngestion(
            @RequestParam String userId,
            @RequestParam(defaultValue = "20") int limit) {
        
        logger.info("Manual Twitter user ingestion triggered for user: {}", userId);
        
        return ingestionService.ingestFromUser(userId, limit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Twitter user ingestion completed");
                response.put("tweetsIngested", count);
                response.put("userId", userId);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Twitter user ingestion failed for user {}: {}", userId, error.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Twitter user ingestion failed: " + error.getMessage());
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Test ingestion with default queries
     */
    @PostMapping("/test")
    public Mono<ResponseEntity<Map<String, Object>>> testIngestion() {
        logger.info("Test Twitter ingestion triggered");
        
        return ingestionService.testIngestion()
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Test Twitter ingestion completed");
                response.put("tweetsIngested", count);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Test Twitter ingestion failed: {}", error.getMessage());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Test Twitter ingestion failed");
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Get Twitter ingestion statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        logger.debug("Twitter stats endpoint accessed");
        
        try {
            IngestionStats stats = ingestionService.getIngestionStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("totalPosts", stats.getTotalPosts());
            response.put("twitterPosts", stats.getTwitterPosts());
            response.put("recentPosts24h", stats.getRecentPosts());
            response.put("sessionTotal", stats.getSessionTotal());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving Twitter stats: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve Twitter statistics");
            
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
        response.put("service", "Twitter Ingestion Service");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get sample Twitter API queries for reference
     */
    @GetMapping("/queries/examples")
    public ResponseEntity<Map<String, Object>> getExampleQueries() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("examples", Map.of(
            "technology", "General technology discussions",
            "ai OR \"artificial intelligence\"", "AI-related content",
            "startup AND funding", "Startup funding news",
            "from:username", "Tweets from specific user",
            "programming -is:retweet", "Programming content excluding retweets",
            "\"machine learning\" lang:en", "English machine learning content"
        ));
        response.put("note", "These are example queries for the Twitter search API");
        
        return ResponseEntity.ok(response);
    }
}