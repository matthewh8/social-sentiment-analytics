package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.repository.SentimentDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
public class SentimentAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SentimentAnalysisService.class);
    
    @Autowired
    private SentimentDataRepository sentimentRepository;
    
    @Autowired(required = false) // Optional - works without Redis
    private RedisCacheService cacheService;
    
    // ===== CONFIGURATION FOR MULTIPLE LIBRARIES =====
    
    // Simple keyword-based analysis (fallback when libraries fail)
    private static final Pattern POSITIVE_WORDS = Pattern.compile(
        "\\b(good|great|excellent|amazing|awesome|love|best|perfect|wonderful|fantastic|brilliant|outstanding|" +
        "incredible|superb|marvelous|terrific|fabulous|magnificent|exceptional|remarkable|impressive|delightful)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern NEGATIVE_WORDS = Pattern.compile(
        "\\b(bad|terrible|awful|hate|worst|horrible|disgusting|pathetic|useless|stupid|annoying|disappointing|" +
        "frustrating|irritating|ridiculous|absurd|dreadful|appalling|atrocious|abysmal|deplorable|reprehensible)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // TODO: Initialize VDurmont library when added
    // private SentimentAnalyzer vdurmontAnalyzer;
    
    // TODO: Initialize Stanford NLP when added (for future switching)
    // private StanfordCoreNLP stanfordPipeline;
    
    // ===== PUBLIC API METHODS =====
    
    /**
     * Analyze sentiment for a social post with caching (async)
     */
    @Async
    public CompletableFuture<SentimentData> analyzeSentimentAsync(SocialPost post) {
        return CompletableFuture.supplyAsync(() -> analyzeSentiment(post));
    }
    
    /**
     * Analyze sentiment for a social post with caching (sync)
     */
    public SentimentData analyzeSentiment(SocialPost post) {
        // Check if sentiment already exists
        Optional<SentimentData> existing = sentimentRepository.findBySocialPostId(post.getId());
        if (existing.isPresent()) {
            logger.debug("Sentiment already exists for post {}", post.getId());
            return existing.get();
        }
        
        // Check cache first (if Redis available)
        String cacheKey = "sentiment:" + post.getId();
        if (cacheService != null && cacheService.isRedisAvailable()) {
            try {
                // Note: You'd implement getCachedSentiment in RedisCacheService
                // For now, we'll skip cache lookup to avoid implementation complexity
                logger.debug("Cache lookup for sentiment of post {} (not implemented yet)", post.getId());
            } catch (Exception e) {
                logger.debug("Cache lookup failed: {}", e.getMessage());
            }
        }
        
        // Perform sentiment analysis
        String content = buildAnalysisText(post);
        SentimentResult result = performSentimentAnalysis(content);
        
        // Create and save sentiment data
        SentimentData sentimentData = new SentimentData();
        sentimentData.setSocialPost(post);
        sentimentData.setSentimentLabel(result.label);
        sentimentData.setSentimentScore(result.score);
        sentimentData.setConfidence(result.confidence);
        sentimentData.setProcessedAt(LocalDateTime.now());
        
        try {
            SentimentData saved = sentimentRepository.save(sentimentData);
            
            // Cache the result (if Redis available)
            if (cacheService != null && cacheService.isRedisAvailable()) {
                try {
                    // Note: You'd implement cacheSentimentResult in RedisCacheService
                    logger.debug("Caching sentiment result for post {} (not implemented yet)", post.getId());
                } catch (Exception e) {
                    logger.debug("Failed to cache sentiment result: {}", e.getMessage());
                }
            }
            
            logger.debug("Sentiment analyzed for post {}: {} (score: {}, confidence: {})", 
                        post.getId(), result.label, result.score, result.confidence);
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to save sentiment data for post {}: {}", post.getId(), e.getMessage());
            throw new RuntimeException("Failed to save sentiment analysis", e);
        }
    }
    
    /**
     * Process sentiment for posts without sentiment analysis
     */
    public Mono<Integer> processPendingSentiments() {
        return Mono.fromCallable(() -> {
            List<SocialPost> postsWithoutSentiment = sentimentRepository.findPostsWithoutSentiment();
            
            if (postsWithoutSentiment.isEmpty()) {
                logger.info("No posts found without sentiment analysis");
                return 0;
            }
            
            logger.info("Processing sentiment for {} posts", postsWithoutSentiment.size());
            
            int processedCount = 0;
            for (SocialPost post : postsWithoutSentiment) {
                try {
                    analyzeSentiment(post);
                    processedCount++;
                    
                    // Small delay to avoid overwhelming the database
                    if (processedCount % 10 == 0) {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to analyze sentiment for post {}: {}", post.getId(), e.getMessage());
                }
            }
            
            logger.info("Completed sentiment processing: {}/{} posts processed successfully", 
                       processedCount, postsWithoutSentiment.size());
            return processedCount;
        });
    }
    
    /**
     * Test sentiment analysis with custom text (for debugging)
     */
    public SentimentResult testSentimentAnalysis(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new SentimentResult(SentimentLabel.NEUTRAL, 0.0, 0.5);
        }
        
        return performSentimentAnalysis(text);
    }
    
    // ===== PRIVATE ANALYSIS METHODS =====
    
    /**
     * Build text for analysis from social post
     */
    private String buildAnalysisText(SocialPost post) {
        StringBuilder text = new StringBuilder();
        
        // Add title (usually most important)
        if (post.getTitle() != null && !post.getTitle().trim().isEmpty()) {
            text.append(post.getTitle()).append(" ");
        }
        
        // Add content if available
        if (post.getContent() != null && !post.getContent().trim().isEmpty()) {
            // Limit content length to avoid overwhelming the analysis
            String content = post.getContent();
            if (content.length() > 1000) {
                content = content.substring(0, 1000) + "...";
            }
            text.append(content);
        }
        
        return text.toString().trim();
    }
    
    /**
     * Perform sentiment analysis using available libraries
     */
    private SentimentResult performSentimentAnalysis(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new SentimentResult(SentimentLabel.NEUTRAL, 0.0, 0.5);
        }
        
        // Method 1: Try VDurmont library (when added)
        try {
            return analyzeWithVDurmont(text);
        } catch (Exception e) {
            logger.debug("VDurmont analysis failed: {}", e.getMessage());
        }
        
        // Method 2: Try Stanford NLP (when added)
        try {
            return analyzeWithStanford(text);
        } catch (Exception e) {
            logger.debug("Stanford NLP analysis failed: {}", e.getMessage());
        }
        
        // Method 3: Fallback to keyword-based analysis
        logger.debug("Using fallback keyword-based sentiment analysis");
        return analyzeWithKeywords(text);
    }
    
    /**
     * VDurmont sentiment analysis (to be implemented when library is added)
     */
    private SentimentResult analyzeWithVDurmont(String text) {
        // TODO: Implement when VDurmont library is added
        // Example implementation:
        // SentimentAnalyzer analyzer = new SentimentAnalyzer();
        // SentimentScore score = analyzer.analyzeSentiment(text);
        // return convertVDurmontResult(score);
        
        throw new UnsupportedOperationException("VDurmont library not yet integrated");
    }
    
    /**
     * Stanford NLP sentiment analysis (to be implemented when library is added)
     */
    private SentimentResult analyzeWithStanford(String text) {
        // TODO: Implement when Stanford NLP is added
        // Example implementation:
        // Annotation annotation = stanfordPipeline.process(text);
        // List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        // return convertStanfordResult(sentences);
        
        throw new UnsupportedOperationException("Stanford NLP library not yet integrated");
    }
    
    /**
     * Keyword-based sentiment analysis (current working implementation)
     */
    private SentimentResult analyzeWithKeywords(String text) {
        String lowerText = text.toLowerCase();
        
        // Count positive and negative words
        long positiveCount = POSITIVE_WORDS.matcher(lowerText).results().count();
        long negativeCount = NEGATIVE_WORDS.matcher(lowerText).results().count();
        
        // Calculate score
        double totalWords = positiveCount + negativeCount;
        double score;
        
        if (totalWords == 0) {
            // No sentiment words found
            score = 0.0;
        } else {
            // Score ranges from -1 (all negative) to +1 (all positive)
            score = (positiveCount - negativeCount) / totalWords;
        }
        
        // Determine label
        SentimentLabel label;
        if (score > 0.2) {
            label = SentimentLabel.POSITIVE;
        } else if (score < -0.2) {
            label = SentimentLabel.NEGATIVE;
        } else {
            label = SentimentLabel.NEUTRAL;
        }
        
        // Calculate confidence based on total sentiment words found and text length
        double wordCount = text.split("\\s+").length;
        double sentimentWordRatio = totalWords / Math.max(wordCount, 1);
        double confidence = Math.min(0.9, 0.3 + (sentimentWordRatio * 2) + (Math.abs(score) * 0.4));
        
        return new SentimentResult(label, score, confidence);
    }
    
    // ===== RESULT CLASS =====
    
    /**
     * Internal class for sentiment analysis results
     */
    public static class SentimentResult {
        public final SentimentLabel label;
        public final double score;
        public final double confidence;
        
        public SentimentResult(SentimentLabel label, double score, double confidence) {
            this.label = label;
            this.score = score;
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return String.format("SentimentResult{label=%s, score=%.3f, confidence=%.3f}", 
                               label, score, confidence);
        }
    }
}