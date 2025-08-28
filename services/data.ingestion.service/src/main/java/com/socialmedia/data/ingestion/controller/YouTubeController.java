package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.service.YouTubeIngestionService;
import com.socialmedia.data.ingestion.service.YouTubeIngestionService.IngestionStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for YouTube data ingestion operations
 * Mirrors RedditController patterns exactly
 * Provides endpoints for manual triggering, monitoring, and statistics
 */
@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeController.class);
    
    private final YouTubeIngestionService ingestionService;
    
    public YouTubeController(YouTubeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }
    
    /**
     * Trigger manual ingestion for specific channels
     */
    @PostMapping("/ingest/channel/{channelId}")
    public Mono<ResponseEntity<Map<String, Object>>> triggerChannelIngestion(
            @PathVariable String channelId,
            @RequestParam(defaultValue = "25") int limit) {
        
        logger.info("Manual channel ingestion triggered via API for channel: {}", channelId);
        
        return ingestionService.ingestFromChannel(channelId, limit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Channel ingestion completed");
                response.put("videosIngested", count);
                response.put("channelId", channelId);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Channel ingestion failed");
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Trigger manual ingestion for multiple channels
     */
    @PostMapping("/ingest")
    public Mono<ResponseEntity<Map<String, Object>>> triggerIngestion(
            @RequestParam(defaultValue = "UCBJycsmduvYEL83R_U4JriQ,UCXuqSBlHAE6Xw-yeJA0Tunw") String channels,
            @RequestParam(defaultValue = "25") int videosPerChannel) {
        
        logger.info("Manual ingestion triggered via API for channels: {}", channels);
        
        String[] channelArray = channels.split(",");
        
        return ingestionService.triggerManualIngestion(channelArray, videosPerChannel)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Ingestion completed");
                response.put("videosIngested", count);
                response.put("channels", channelArray);
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
     * Trigger search-based ingestion
     */
    @PostMapping("/ingest/search")
    public Mono<ResponseEntity<Map<String, Object>>> triggerSearchIngestion(
            @RequestBody SearchRequest searchRequest) {
        
        logger.info("Search ingestion triggered via API for query: '{}'", searchRequest.getQuery());
        
        return ingestionService.ingestFromSearch(searchRequest.getQuery(), searchRequest.getLimit())
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Search ingestion completed");
                response.put("videosIngested", count);
                response.put("query", searchRequest.getQuery());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Search ingestion failed");
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }
    
    /**
     * Ingest trending videos
     */
    @PostMapping("/trending")
    public Mono<ResponseEntity<Map<String, Object>>> ingestTrending(
            @RequestParam(defaultValue = "50") int limit) {
        
        logger.info("Trending videos ingestion triggered via API, limit: {}", limit);
        
        return ingestionService.ingestTrendingVideos(limit)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Trending videos ingestion completed");
                response.put("videosIngested", count);
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
        logger.debug("YouTube stats endpoint accessed");
        
        try {
            IngestionStats stats = ingestionService.getIngestionStats();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalPosts", stats.getTotalPosts());
            statistics.put("youtubePosts", stats.getYoutubePosts());
            statistics.put("recentPosts24h", stats.getRecentPosts());
            statistics.put("sessionTotal", stats.getSessionTotal());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("statistics", statistics);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving YouTube stats: {}", e.getMessage());
            
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
        response.put("service", "YouTube Data Ingestion");
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
        response.put("quotaLimitPerDay", 10000);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple DTO for search requests
     */
    public static class SearchRequest {
        private String query;
        private int limit = 25;
        
        public SearchRequest() {}
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
}