package com.socialmedia.data.ingestion.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.data.ingestion.dto.AnalyticsReport;
import com.socialmedia.data.ingestion.dto.EngagementStats;
import com.socialmedia.data.ingestion.dto.SocialPostDto;
import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RedisCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String CACHE_PREFIX = "social_media:";
    
    // Cache key patterns
    private static final String STATS_KEY = CACHE_PREFIX + "stats";
    private static final String PLATFORM_STATS_KEY = CACHE_PREFIX + "platform_stats:{}";
    private static final String TRENDING_POSTS_KEY = CACHE_PREFIX + "trending:{}:{}";
    private static final String ANALYTICS_REPORT_KEY = CACHE_PREFIX + "analytics:{}:{}";
    private static final String API_RESPONSE_KEY = CACHE_PREFIX + "api_response:{}:{}";
    private static final String SEARCH_RESULTS_KEY = CACHE_PREFIX + "search:{}:{}";
    
    // Cache TTL durations
    private static final Duration STATS_TTL = Duration.ofMinutes(5);
    private static final Duration TRENDING_TTL = Duration.ofMinutes(15);
    private static final Duration ANALYTICS_TTL = Duration.ofMinutes(30);
    private static final Duration API_RESPONSE_TTL = Duration.ofMinutes(10);
    private static final Duration SEARCH_TTL = Duration.ofMinutes(20);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // ===== STATISTICS CACHING =====
    
    /**
     * Cache ingestion statistics with 5-minute TTL
     */
    public void cacheStats(Map<String, Object> stats) {
        try {
            redisTemplate.opsForValue().set(STATS_KEY, stats, STATS_TTL);
            logger.debug("Cached ingestion statistics");
        } catch (Exception e) {
            logger.warn("Failed to cache statistics: {}", e.getMessage());
        }
    }
    
    /**
     * Retrieve cached statistics
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getCachedStats() {
        try {
            Object cached = redisTemplate.opsForValue().get(STATS_KEY);
            if (cached instanceof Map) {
                logger.debug("Retrieved cached statistics");
                return Optional.of((Map<String, Object>) cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached statistics: {}", e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Cache platform-specific statistics
     */
    public void cachePlatformStats(Platform platform, Map<String, Object> stats) {
        try {
            String key = PLATFORM_STATS_KEY.replace("{}", platform.name().toLowerCase());
            redisTemplate.opsForValue().set(key, stats, STATS_TTL);
            logger.debug("Cached {} platform statistics", platform);
        } catch (Exception e) {
            logger.warn("Failed to cache {} platform statistics: {}", platform, e.getMessage());
        }
    }
    
    /**
     * Retrieve cached platform statistics
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getCachedPlatformStats(Platform platform) {
        try {
            String key = PLATFORM_STATS_KEY.replace("{}", platform.name().toLowerCase());
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Map) {
                logger.debug("Retrieved cached {} platform statistics", platform);
                return Optional.of((Map<String, Object>) cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached {} platform statistics: {}", platform, e.getMessage());
        }
        return Optional.empty();
    }
    
    // ===== TRENDING POSTS CACHING =====
    
    /**
     * Cache trending posts with 15-minute TTL
     */
    public void cacheTrendingPosts(Platform platform, int limit, List<SocialPostDto> posts) {
        try {
            String key = TRENDING_POSTS_KEY.replace("{}", platform.name().toLowerCase())
                                          .replace("{}", String.valueOf(limit));
            redisTemplate.opsForValue().set(key, posts, TRENDING_TTL);
            logger.debug("Cached {} trending posts for {} (limit: {})", posts.size(), platform, limit);
        } catch (Exception e) {
            logger.warn("Failed to cache trending posts for {}: {}", platform, e.getMessage());
        }
    }
    
    /**
     * Retrieve cached trending posts
     */
    @SuppressWarnings("unchecked")
    public Optional<List<SocialPostDto>> getCachedTrendingPosts(Platform platform, int limit) {
        try {
            String key = TRENDING_POSTS_KEY.replace("{}", platform.name().toLowerCase())
                                          .replace("{}", String.valueOf(limit));
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                logger.debug("Retrieved cached trending posts for {} (limit: {})", platform, limit);
                return Optional.of((List<SocialPostDto>) cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached trending posts for {}: {}", platform, e.getMessage());
        }
        return Optional.empty();
    }
    
    // ===== ANALYTICS REPORT CACHING =====
    
    /**
     * Cache analytics report with 30-minute TTL
     */
    public void cacheAnalyticsReport(LocalDateTime start, LocalDateTime end, AnalyticsReport report) {
        try {
            String key = ANALYTICS_REPORT_KEY.replace("{}", start.toString())
                                            .replace("{}", end.toString());
            redisTemplate.opsForValue().set(key, report, ANALYTICS_TTL);
            logger.debug("Cached analytics report for period {} to {}", start, end);
        } catch (Exception e) {
            logger.warn("Failed to cache analytics report: {}", e.getMessage());
        }
    }
    
    /**
     * Retrieve cached analytics report
     */
    public Optional<AnalyticsReport> getCachedAnalyticsReport(LocalDateTime start, LocalDateTime end) {
        try {
            String key = ANALYTICS_REPORT_KEY.replace("{}", start.toString())
                                            .replace("{}", end.toString());
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof AnalyticsReport) {
                logger.debug("Retrieved cached analytics report for period {} to {}", start, end);
                return Optional.of((AnalyticsReport) cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached analytics report: {}", e.getMessage());
        }
        return Optional.empty();
    }
    
    // ===== API RESPONSE CACHING =====
    
    /**
     * Cache external API responses (Reddit/YouTube) with 10-minute TTL
     */
    public void cacheApiResponse(String apiType, String identifier, Object response) {
        try {
            String key = API_RESPONSE_KEY.replace("{}", apiType.toLowerCase())
                                        .replace("{}", identifier);
            redisTemplate.opsForValue().set(key, response, API_RESPONSE_TTL);
            logger.debug("Cached {} API response for identifier: {}", apiType, identifier);
        } catch (Exception e) {
            logger.warn("Failed to cache {} API response for {}: {}", apiType, identifier, e.getMessage());
        }
    }
    
    /**
     * Retrieve cached API response
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCachedApiResponse(String apiType, String identifier, Class<T> responseType) {
        try {
            String key = API_RESPONSE_KEY.replace("{}", apiType.toLowerCase())
                                        .replace("{}", identifier);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && responseType.isInstance(cached)) {
                logger.debug("Retrieved cached {} API response for identifier: {}", apiType, identifier);
                return Optional.of(responseType.cast(cached));
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached {} API response for {}: {}", apiType, identifier, e.getMessage());
        }
        return Optional.empty();
    }
    
    // ===== SEARCH RESULTS CACHING =====
    
    /**
     * Cache search results with 20-minute TTL
     */
    public void cacheSearchResults(String query, int limit, List<SocialPostDto> results) {
        try {
            String key = SEARCH_RESULTS_KEY.replace("{}", sanitizeKey(query))
                                          .replace("{}", String.valueOf(limit));
            redisTemplate.opsForValue().set(key, results, SEARCH_TTL);
            logger.debug("Cached {} search results for query: '{}' (limit: {})", results.size(), query, limit);
        } catch (Exception e) {
            logger.warn("Failed to cache search results for query '{}': {}", query, e.getMessage());
        }
    }
    
    /**
     * Retrieve cached search results
     */
    @SuppressWarnings("unchecked")
    public Optional<List<SocialPostDto>> getCachedSearchResults(String query, int limit) {
        try {
            String key = SEARCH_RESULTS_KEY.replace("{}", sanitizeKey(query))
                                          .replace("{}", String.valueOf(limit));
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof List) {
                logger.debug("Retrieved cached search results for query: '{}' (limit: {})", query, limit);
                return Optional.of((List<SocialPostDto>) cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve cached search results for query '{}': {}", query, e.getMessage());
        }
        return Optional.empty();
    }
    
    // ===== CACHE MANAGEMENT =====
    
    /**
     * Invalidate all statistics caches (called after new data ingestion)
     */
    public void invalidateStatsCaches() {
        try {
            // Invalidate general stats
            redisTemplate.delete(STATS_KEY);
            
            // Invalidate platform-specific stats
            for (Platform platform : Platform.values()) {
                String key = PLATFORM_STATS_KEY.replace("{}", platform.name().toLowerCase());
                redisTemplate.delete(key);
            }
            
            logger.debug("Invalidated statistics caches after data ingestion");
        } catch (Exception e) {
            logger.warn("Failed to invalidate statistics caches: {}", e.getMessage());
        }
    }
    
    /**
     * Invalidate trending posts cache for specific platform
     */
    public void invalidateTrendingCache(Platform platform) {
        try {
            String pattern = TRENDING_POSTS_KEY.replace("{}", platform.name().toLowerCase())
                                              .replace("{}", "*");
            // Note: In production, consider using SCAN instead of KEYS for large datasets
            redisTemplate.delete(redisTemplate.keys(pattern));
            logger.debug("Invalidated trending posts cache for {}", platform);
        } catch (Exception e) {
            logger.warn("Failed to invalidate trending cache for {}: {}", platform, e.getMessage());
        }
    }
    
    /**
     * Get cache health information
     */
    public Map<String, Object> getCacheHealth() {
        try {
            boolean isConnected = redisTemplate.getConnectionFactory()
                                              .getConnection()
                                              .ping() != null;
            
            return Map.of(
                "connected", isConnected,
                "keysCount", redisTemplate.keys(CACHE_PREFIX + "*").size(),
                "statsKeyExists", redisTemplate.hasKey(STATS_KEY),
                "lastCheck", LocalDateTime.now()
            );
        } catch (Exception e) {
            logger.error("Failed to check cache health: {}", e.getMessage());
            return Map.of(
                "connected", false,
                "error", e.getMessage(),
                "lastCheck", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Clear all application caches (use with caution)
     */
    public void clearAllCaches() {
        try {
            redisTemplate.delete(redisTemplate.keys(CACHE_PREFIX + "*"));
            logger.info("Cleared all application caches");
        } catch (Exception e) {
            logger.error("Failed to clear all caches: {}", e.getMessage());
        }
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Sanitize cache key to avoid Redis key issues
     */
    private String sanitizeKey(String input) {
        return input.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
    
    /**
     * Check if Redis is available
     */
    public boolean isRedisAvailable() {
        try {
            return redisTemplate.getConnectionFactory()
                               .getConnection()
                               .ping() != null;
        } catch (Exception e) {
            logger.debug("Redis not available: {}", e.getMessage());
            return false;
        }
    }
}