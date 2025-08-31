package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.config.RedditApiConfig;
import com.socialmedia.data.ingestion.service.RedditIngestionService;
import com.socialmedia.data.ingestion.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reddit")
@CrossOrigin(origins = "*")
public class RedditController {
    
    private static final Logger logger = LoggerFactory.getLogger(RedditController.class);
    
    @Autowired
    private RedditIngestionService redditService;
    
    @Autowired
    private RedditApiConfig config;
    
    @Autowired(required = false) // Optional - works without Redis
    private RedisCacheService cacheService;
    
    // ===== EXISTING ENDPOINTS (ENHANCED WITH CACHING) =====
    
    /**
     * Health check (enhanced with cache status)
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "Reddit Data Ingestion",
                "caching", Map.of(
                    "enabled", cacheService != null && cacheService.isRedisAvailable(),
                    "available", cacheService != null ? cacheService.isRedisAvailable() : false
                ),
                "timestamp", System.currentTimeMillis()
            );
            
            logger.debug("Reddit health check completed");
            return ResponseEntity.ok(health);
        });
    }
    
    /**
     * Get statistics (enhanced with caching)
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStats() {
        return redditService.getIngestionStats()
            .map(stats -> {
                logger.debug("Retrieved Reddit statistics");
                return ResponseEntity.ok(stats);
            })
            .onErrorResume(error -> {
                logger.error("Failed to get Reddit statistics: {}", error.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(Map.of(
                        "status", "error",
                        "message", "Failed to retrieve statistics",
                        "timestamp", System.currentTimeMillis()
                    )));
            });
    }
    
    /**
     * Manual ingestion trigger (enhanced with caching)
     */
    @PostMapping("/ingest")
    public Mono<ResponseEntity<Map<String, Object>>> triggerIngestion(
            @RequestParam(defaultValue = "technology,programming") String subreddits,
            @RequestParam(defaultValue = "25") int postsPerSubreddit) {
        
        List<String> subredditList = Arrays.asList(subreddits.split(","));
        
        logger.info("Manual ingestion triggered for subreddits: {} (posts per subreddit: {})", 
            subreddits, postsPerSubreddit);
        
        return redditService.triggerManualIngestion(subredditList, postsPerSubreddit)
            .map(response -> {
                logger.info("Manual ingestion completed: {}", response);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Manual ingestion failed: {}", error.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(Map.of(
                        "status", "error",
                        "message", "Ingestion failed: " + error.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    )));
            });
    }
    
    /**
     * Ingest trending posts (enhanced with caching)
     */
    @PostMapping("/trending")
    public Mono<ResponseEntity<Map<String, Object>>> ingestTrendingPosts(
            @RequestParam(defaultValue = "50") int limit) {
        
        logger.info("Trending posts ingestion triggered (limit: {})", limit);
        
        return redditService.ingestFromSubreddit("popular", limit)
            .map(postsIngested -> {
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Trending posts ingestion completed",
                    "postsIngested", postsIngested,
                    "source", "r/popular",
                    "timestamp", System.currentTimeMillis()
                );
                
                logger.info("Trending posts ingestion completed: {} posts", postsIngested);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Trending posts ingestion failed: {}", error.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(Map.of(
                        "status", "error",
                        "message", "Trending ingestion failed: " + error.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    )));
            });
    }
    
    /**
     * Get service configuration (enhanced with cache info)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> configInfo = Map.of(
            "schedulingEnabled", false,
            "schedulingInterval", "Manual",
            "rateLimitEnabled", true,
            "retryEnabled", true,
            "maxRetries", 2,
            "cachingEnabled", cacheService != null && cacheService.isRedisAvailable(),
            "defaultSubreddits", Arrays.asList(config.getDefaultSubreddits()),
            "requestsPerMinute", config.getRequestsPerMinute()
        );
        
        logger.debug("Retrieved Reddit service configuration");
        return ResponseEntity.ok(configInfo);
    }
    
    // ===== NEW CACHE MANAGEMENT ENDPOINTS (OPTIONAL) =====
    
    /**
     * Clear Reddit caches (only works if Redis available)
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> clearCaches() {
        if (cacheService != null && cacheService.isRedisAvailable()) {
            try {
                cacheService.invalidateStatsCaches();
                
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Reddit caches cleared successfully",
                    "timestamp", System.currentTimeMillis()
                );
                
                logger.info("Reddit caches cleared successfully");
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                logger.error("Failed to clear caches: {}", e.getMessage());
                return ResponseEntity.status(500)
                    .body(Map.of(
                        "status", "error",
                        "message", "Failed to clear caches: " + e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    ));
            }
        } else {
            return ResponseEntity.ok(Map.of(
                "status", "info",
                "message", "Redis not available - no caches to clear",
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get cache health (only works if Redis available)
     */
    @GetMapping("/cache/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        if (cacheService != null) {
            Map<String, Object> cacheHealth = cacheService.getCacheHealth();
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "cacheHealth", cacheHealth,
                "timestamp", System.currentTimeMillis()
            );
            
            logger.debug("Retrieved cache health information");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(Map.of(
                "status", "info",
                "message", "Redis cache service not available",
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    // ===== SIMPLE TEST ENDPOINT =====
    
    /**
     * Simple test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = Map.of(
            "message", "Reddit Controller is working!",
            "cacheAvailable", cacheService != null && cacheService.isRedisAvailable(),
            "availableEndpoints", List.of(
                "GET /api/reddit/health",
                "GET /api/reddit/stats", 
                "POST /api/reddit/ingest",
                "POST /api/reddit/trending",
                "GET /api/reddit/config"
            ),
            "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(response);
    }
}