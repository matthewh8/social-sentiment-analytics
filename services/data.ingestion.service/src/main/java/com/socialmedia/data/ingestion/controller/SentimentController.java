package com.socialmedia.data.ingestion.controller;

import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.socialmedia.data.ingestion.repository.SentimentDataRepository;
import com.socialmedia.data.ingestion.service.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
    
    @Autowired
    private SentimentAnalysisService sentimentService;
    
    @Autowired
    private SentimentDataRepository sentimentRepository;
    
    /**
     * Health check for sentiment service
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        return Mono.fromCallable(() -> {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("service", "Sentiment Analysis Service");
            response.put("timestamp", System.currentTimeMillis());
            
            // Check if we have any sentiment data
            long sentimentCount = sentimentRepository.count();
            response.put("totalSentiments", sentimentCount);
            
            return ResponseEntity.ok(response);
        });
    }
    
    /**
     * Get sentiment statistics
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<Map<String, Object>>> getStats() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // Total counts by sentiment
            long totalSentiments = sentimentRepository.count();
            long positiveCount = sentimentRepository.countBySentimentLabel(SentimentLabel.POSITIVE);
            long negativeCount = sentimentRepository.countBySentimentLabel(SentimentLabel.NEGATIVE);
            long neutralCount = sentimentRepository.countBySentimentLabel(SentimentLabel.NEUTRAL);
            
            stats.put("totalSentiments", totalSentiments);
            stats.put("positive", positiveCount);
            stats.put("negative", negativeCount);
            stats.put("neutral", neutralCount);
            
            // Percentages
            if (totalSentiments > 0) {
                stats.put("positivePercentage", (positiveCount * 100.0) / totalSentiments);
                stats.put("negativePercentage", (negativeCount * 100.0) / totalSentiments);
                stats.put("neutralPercentage", (neutralCount * 100.0) / totalSentiments);
            }
            
            // Recent sentiment (last 24 hours)
            LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
            long recentSentiments = sentimentRepository.countByProcessedAtAfter(dayAgo);
            stats.put("recentSentiments24h", recentSentiments);
            
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
        });
    }
    
    /**
     * Process pending sentiments manually
     */
    @PostMapping("/process")
    public Mono<ResponseEntity<Map<String, Object>>> processPendingSentiments() {
        return sentimentService.processPendingSentiments()
            .map(processedCount -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Sentiment processing triggered");
                response.put("processedCount", processedCount);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Sentiment processing failed: " + error.getMessage());
                errorResponse.put("timestamp", System.currentTimeMillis());
                
                return Mono.just(ResponseEntity.internalServerError().body(errorResponse));
            });
    }
    
    /**
     * Get sentiment breakdown by platform
     */
    @GetMapping("/breakdown")
    public Mono<ResponseEntity<Map<String, Object>>> getSentimentBreakdown() {
        return Mono.fromCallable(() -> {
            Map<String, Object> breakdown = new HashMap<>();
            
            // This would require a custom query to join with social_posts
            // For now, return basic breakdown
            List<Object[]> sentimentBreakdown = sentimentRepository.getSentimentBreakdown();
            
            Map<String, Map<String, Long>> platformBreakdown = new HashMap<>();
            
            for (Object[] row : sentimentBreakdown) {
                String platform = (String) row[0];
                SentimentLabel sentiment = (SentimentLabel) row[1];
                Long count = (Long) row[2];
                
                platformBreakdown.computeIfAbsent(platform, k -> new HashMap<>())
                                .put(sentiment.name().toLowerCase(), count);
            }
            
            breakdown.put("byPlatform", platformBreakdown);
            breakdown.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(breakdown);
        });
    }
    
    /**
     * Test sentiment analysis with custom text
     */
    @PostMapping("/test")
    public Mono<ResponseEntity<Map<String, Object>>> testSentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        if (text == null || text.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Text is required");
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
        
        return Mono.fromCallable(() -> {
            // Create a dummy post for testing
            // Note: This won't be saved to database
            Map<String, Object> result = new HashMap<>();
            result.put("inputText", text);
            result.put("timestamp", System.currentTimeMillis());
            
            // You would implement test sentiment analysis here
            result.put("sentiment", "neutral");
            result.put("score", 0.0);
            result.put("confidence", 0.5);
            result.put("note", "This is a test endpoint - implement actual analysis");
            
            return ResponseEntity.ok(result);
        });
    }
}