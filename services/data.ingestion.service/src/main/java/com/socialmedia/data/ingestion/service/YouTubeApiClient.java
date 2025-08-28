package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.config.YouTubeApiConfig;
import com.socialmedia.data.ingestion.model.youtube.YouTubeVideo;
import com.socialmedia.data.ingestion.model.youtube.YouTubeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Working YouTube API Client with proper search functionality
 * Handles YouTube's complex API structure correctly
 */
@Service
public class YouTubeApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeApiClient.class);
    
    private final WebClient webClient;
    private final RateLimiter rateLimiter;
    private final YouTubeApiConfig config;
    
    public YouTubeApiClient(WebClient youtubeWebClient, RateLimiter rateLimiter, YouTubeApiConfig config) {
        this.webClient = youtubeWebClient;
        this.rateLimiter = rateLimiter;
        this.config = config;
    }
    
    /**
     * Search for videos - WORKING VERSION with proper two-step process
     */
    public Flux<YouTubeVideo> searchVideos(String query, int limit, String pageToken) {
        logger.info("Searching {} videos for query: {}", limit, query);
        
        return rateLimiter.acquireToken()
            .then(makeSearchApiCall(query, limit, pageToken))
            .retry(2)
            .flatMapMany(response -> {
                // Extract video IDs from search response
                List<String> videoIds = extractVideoIdsFromSearchResponse(response);
                logger.info("Search found {} video IDs", videoIds.size());
                
                if (videoIds.isEmpty()) {
                    return Flux.empty();
                }
                
                // Fetch full video details with statistics
                return fetchVideoDetailsByIds(videoIds);
            })
            .doOnNext(video -> logger.debug("Found video: {} for query {}", video.getId(), query))
            .doOnError(error -> logger.error("Error searching videos for query {}: {}", query, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Search failed, returning empty flux: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Fetch videos from channel - WORKING VERSION
     */
    public Flux<YouTubeVideo> fetchChannelVideos(String channelId, int limit, String pageToken) {
        logger.info("Fetching {} videos from channel {}", limit, channelId);
        
        return rateLimiter.acquireToken()
            .then(makeChannelSearchCall(channelId, limit, pageToken))
            .retry(2)
            .flatMapMany(response -> {
                // Extract video IDs from channel search response
                List<String> videoIds = extractVideoIdsFromSearchResponse(response);
                logger.info("Channel search found {} video IDs", videoIds.size());
                
                if (videoIds.isEmpty()) {
                    return Flux.empty();
                }
                
                // Fetch full video details with statistics
                return fetchVideoDetailsByIds(videoIds);
            })
            .doOnNext(video -> logger.debug("Fetched video: {} from channel {}", video.getId(), video.getChannelTitle()))
            .doOnError(error -> logger.error("Error fetching videos from channel {}: {}", channelId, error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Channel fetch failed, returning empty flux: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Fetch trending videos (already working)
     */
    public Flux<YouTubeVideo> fetchTrendingVideos(int limit) {
        logger.info("Fetching {} trending videos", limit);
        
        return rateLimiter.acquireToken()
            .then(makeTrendingApiCall(limit))
            .retry(2)
            .flatMapMany(this::extractVideos)
            .doOnNext(video -> logger.debug("Fetched trending video: {}", video.getId()))
            .doOnError(error -> logger.error("Error fetching trending videos: {}", error.getMessage()))
            .onErrorResume(error -> {
                logger.warn("Returning empty flux due to error: {}", error.getMessage());
                return Flux.empty();
            });
    }
    
    /**
     * Fetch from multiple channels
     */
    public Flux<YouTubeVideo> fetchMultipleChannels(java.util.List<String> channelIds, int limitPerChannel) {
        logger.info("Fetching videos from {} channels", channelIds.size());
        
        return Flux.fromIterable(channelIds)
            .flatMap(channelId -> fetchChannelVideos(channelId, limitPerChannel, null)
                .onErrorResume(error -> {
                    logger.warn("Failed to fetch from channel {}: {}", channelId, error.getMessage());
                    return Flux.empty();
                }), 2)
            .doOnComplete(() -> logger.info("Completed fetching from all channels"));
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    /**
     * Extract video IDs from search response (handles search API structure)
     */
    private List<String> extractVideoIdsFromSearchResponse(YouTubeResponse response) {
        if (response == null || response.getItems() == null) {
            return List.of();
        }
        
        return response.getItems().stream()
            .map(video -> {
                // Handle search API response structure: id.videoId
                Object idObj = video.getIdObject();
                if (idObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> idMap = (java.util.Map<String, Object>) idObj;
                    return (String) idMap.get("videoId");
                }
                return null;
            })
            .filter(id -> id != null && !id.trim().isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * Fetch full video details by IDs
     */
    private Flux<YouTubeVideo> fetchVideoDetailsByIds(List<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return Flux.empty();
        }
        
        // Join video IDs (max 50 per request)
        String videoIdString = String.join(",", videoIds);
        logger.info("Fetching details for {} videos", videoIds.size());
        
        return rateLimiter.acquireToken()
            .then(makeVideoDetailsCall(videoIdString))
            .retry(2)
            .flatMapMany(this::extractVideos)
            .doOnError(error -> logger.error("Error fetching video details: {}", error.getMessage()));
    }
    
    /**
     * Make search API call
     */
    private Mono<YouTubeResponse> makeSearchApiCall(String query, int limit, String pageToken) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/search")
                    .queryParam("part", "snippet")
                    .queryParam("q", query)
                    .queryParam("type", "video")
                    .queryParam("order", "relevance")
                    .queryParam("maxResults", Math.min(limit, 50))
                    .queryParam("key", config.getApiKey());
                
                if (pageToken != null && !pageToken.isEmpty()) {
                    builder.queryParam("pageToken", pageToken);
                }
                
                return builder.build();
            })
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by YouTube API");
                return Mono.error(new YouTubeApiException("Rate limited"));
            })
            .onStatus(status -> status.equals(HttpStatus.FORBIDDEN), response -> {
                logger.error("Quota exceeded or invalid API key");
                return Mono.error(new YouTubeApiException("Quota exceeded or invalid API key"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(YouTubeResponse.class)
            .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Make channel search API call
     */
    private Mono<YouTubeResponse> makeChannelSearchCall(String channelId, int limit, String pageToken) {
        return webClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder
                    .path("/search")
                    .queryParam("part", "snippet")
                    .queryParam("channelId", channelId)
                    .queryParam("type", "video")
                    .queryParam("order", "date")
                    .queryParam("maxResults", Math.min(limit, 50))
                    .queryParam("key", config.getApiKey());
                
                if (pageToken != null && !pageToken.isEmpty()) {
                    builder.queryParam("pageToken", pageToken);
                }
                
                return builder.build();
            })
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by YouTube API");
                return Mono.error(new YouTubeApiException("Rate limited"));
            })
            .onStatus(status -> status.equals(HttpStatus.FORBIDDEN), response -> {
                logger.error("Quota exceeded or invalid API key");
                return Mono.error(new YouTubeApiException("Quota exceeded or invalid API key"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(YouTubeResponse.class)
            .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Make video details API call
     */
    private Mono<YouTubeResponse> makeVideoDetailsCall(String videoIds) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/videos")
                .queryParam("part", "snippet,statistics")
                .queryParam("id", videoIds)
                .queryParam("key", config.getApiKey())
                .build())
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by YouTube API");
                return Mono.error(new YouTubeApiException("Rate limited"));
            })
            .onStatus(status -> status.equals(HttpStatus.FORBIDDEN), response -> {
                logger.error("Quota exceeded or invalid API key");
                return Mono.error(new YouTubeApiException("Quota exceeded or invalid API key"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(YouTubeResponse.class)
            .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Make trending API call (this works)
     */
    private Mono<YouTubeResponse> makeTrendingApiCall(int limit) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/videos")
                .queryParam("part", "snippet,statistics")
                .queryParam("chart", "mostPopular")
                .queryParam("regionCode", "US")
                .queryParam("categoryId", "28") // Science & Technology
                .queryParam("maxResults", Math.min(limit, 50))
                .queryParam("key", config.getApiKey())
                .build())
            .retrieve()
            .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response -> {
                logger.warn("Rate limited by YouTube API");
                return Mono.error(new YouTubeApiException("Rate limited"));
            })
            .onStatus(status -> status.equals(HttpStatus.FORBIDDEN), response -> {
                logger.error("Quota exceeded or invalid API key");
                return Mono.error(new YouTubeApiException("Quota exceeded or invalid API key"));
            })
            .onStatus(status -> status.is4xxClientError(), response -> {
                logger.error("Client error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Client error: " + response.statusCode()));
            })
            .onStatus(status -> status.is5xxServerError(), response -> {
                logger.error("Server error from YouTube API: {}", response.statusCode());
                return Mono.error(new YouTubeApiException("Server error: " + response.statusCode()));
            })
            .bodyToMono(YouTubeResponse.class)
            .timeout(Duration.ofSeconds(30));
    }
    
    /**
     * Extract videos from video details API response (has statistics)
     */
    private Flux<YouTubeVideo> extractVideos(YouTubeResponse response) {
        if (response == null || response.getItems() == null) {
            logger.warn("Empty or invalid YouTube response");
            return Flux.empty();
        }
        
        return Flux.fromIterable(response.getItems())
            .filter(this::isValidVideo);
    }
    
    /**
     * Basic video validation
     */
    private boolean isValidVideo(YouTubeVideo video) {
        if (video == null) return false;
        
        // Must have basic required fields
        if (video.getId() == null || video.getId().trim().isEmpty()) {
            return false;
        }
        
        if (video.getSnippet() == null) {
            return false;
        }
        
        // Must have title
        if (video.getSnippet().getTitle() == null || 
            video.getSnippet().getTitle().trim().isEmpty()) {
            return false;
        }
        
        // Must have channel info
        if (video.getSnippet().getChannelTitle() == null || 
            video.getSnippet().getChannelTitle().trim().isEmpty()) {
            return false;
        }
        
        // Filter out private/deleted videos
        if (video.getSnippet().getTitle().equals("[Private video]") ||
            video.getSnippet().getTitle().equals("[Deleted video]")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Simple exception for YouTube API errors
     */
    public static class YouTubeApiException extends RuntimeException {
        public YouTubeApiException(String message) {
            super(message);
        }
    }
}