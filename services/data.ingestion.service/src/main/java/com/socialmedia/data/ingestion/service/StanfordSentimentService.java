package com.socialmedia.data.ingestion.service;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.repository.SentimentDataRepository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class StanfordSentimentService {

    private static final Logger logger = LoggerFactory.getLogger(StanfordSentimentService.class);
    
    @Autowired
    private SentimentDataRepository sentimentDataRepository;
    
    private StanfordCoreNLP pipeline;
    private boolean isInitialized = false;

    @PostConstruct
    public void initializePipeline() {
        try {
            logger.info("Initializing Stanford CoreNLP sentiment analysis pipeline on M1 Mac...");
            logger.info("Java version: {}", System.getProperty("java.version"));
            logger.info("OS arch: {}", System.getProperty("os.arch"));
            logger.info("Available memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
            
            // Check if models are accessible
            try {
                ClassLoader classLoader = this.getClass().getClassLoader();
                if (classLoader.getResource("edu/stanford/nlp/models/sentiment/sentiment.ser.gz") != null) {
                    logger.info("Sentiment model found in classpath");
                } else {
                    logger.warn("Sentiment model NOT found in classpath - this will cause initialization failure");
                }
            } catch (Exception e) {
                logger.warn("Error checking model availability: {}", e.getMessage());
            }
            
            // M1 Mac specific initialization - using default models only
            Properties props = new Properties();
            
            // FIXED: Use default models, don't specify custom paths
            props.setProperty("annotators", "tokenize,ssplit,pos,parse,sentiment");
            props.setProperty("tokenize.language", "en");
            props.setProperty("ssplit.isOneSentence", "false");
            
            // Only specify sentiment model path - let others use defaults
            props.setProperty("sentiment.model", "edu/stanford/nlp/models/sentiment/sentiment.ser.gz");
            
            // M1 performance optimizations
            props.setProperty("threads", "1");
            props.setProperty("ner.useSUTime", "false");
            props.setProperty("ner.applyNumericClassifiers", "false");
            props.setProperty("enforceRequirements", "false");
            props.setProperty("outputFormat", "text");
            
            // Memory constraints for M1 with parser
            props.setProperty("parse.maxlen", "25");
            
            // Let POS and parse use default models - don't override paths
            
            // M1 native library settings
            System.setProperty("jna.nosys", "true");
            System.setProperty("java.awt.headless", "true");
            
            logger.info("Creating Stanford CoreNLP pipeline with M1-specific settings...");
            logger.info("Using properties: {}", props.toString());
            
            long startTime = System.currentTimeMillis();
            this.pipeline = new StanfordCoreNLP(props);
            long initTime = System.currentTimeMillis() - startTime;
            
            this.isInitialized = true;
            logger.info("Stanford CoreNLP pipeline initialized successfully on M1 Mac in {} ms", initTime);
            
        } catch (OutOfMemoryError e) {
            logger.error("M1 Mac: Out of memory during Stanford CoreNLP initialization. Try increasing heap size to 4GB+");
            logger.error("Add this to your run command: -Xmx4g");
            this.isInitialized = false;
        } catch (UnsatisfiedLinkError e) {
            logger.error("M1 Mac: Native library loading error - {}", e.getMessage());
            logger.error("This is a known M1 compatibility issue with Stanford CoreNLP");
            this.isInitialized = false;
        } catch (Exception e) {
            logger.error("Failed to initialize Stanford CoreNLP pipeline on M1 Mac: {}", e.getMessage());
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Full stack trace:", e);
            
            // Print system info for debugging
            logger.error("System debugging info:");
            logger.error("  Java vendor: {}", System.getProperty("java.vendor"));
            logger.error("  Java home: {}", System.getProperty("java.home"));
            logger.error("  Classpath: {}", System.getProperty("java.class.path"));
            
            this.isInitialized = false;
            logger.warn("Sentiment analysis will use fallback mode due to M1 initialization failure");
        }
    }

    public SentimentData analyzeSentiment(SocialPost socialPost) {
        if (!isInitialized) {
            logger.warn("Stanford CoreNLP not initialized, returning neutral sentiment");
            return createFallbackSentiment(socialPost);
        }

        try {
            String text = socialPost.getContent();
            if (text == null || text.trim().isEmpty()) {
                text = socialPost.getTitle();
            }
            
            if (text.length() > 1000) {
                text = text.substring(0, 1000);
            }

            Annotation annotation = new Annotation(text);
            pipeline.annotate(annotation);

            List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
            
            if (sentences == null || sentences.isEmpty()) {
                return createFallbackSentiment(socialPost);
            }

            double totalScore = 0.0;
            int sentenceCount = 0;
            int positiveCount = 0;
            int negativeCount = 0;
            int neutralCount = 0;

            for (CoreMap sentence : sentences) {
                Tree tree = sentence.get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                if (tree != null) {
                    int sentimentValue = RNNCoreAnnotations.getPredictedClass(tree);
                    double normalizedScore = (sentimentValue - 2.0) / 2.0;
                    totalScore += normalizedScore;
                    sentenceCount++;
                    
                    if (sentimentValue <= 1) negativeCount++;
                    else if (sentimentValue >= 3) positiveCount++;
                    else neutralCount++;
                }
            }

            if (sentenceCount == 0) {
                return createFallbackSentiment(socialPost);
            }

            double averageScore = totalScore / sentenceCount;
            double confidence = calculateConfidence(positiveCount, negativeCount, neutralCount);
            SentimentLabel label = determineSentimentLabel(averageScore, confidence);

            SentimentData sentiment = createSentimentData(socialPost, label, averageScore, confidence);
            
            // Save to database
            return sentimentDataRepository.save(sentiment);

        } catch (Exception e) {
            logger.error("Error analyzing sentiment for post {}: {}", socialPost.getExternalId(), e.getMessage());
            return createFallbackSentiment(socialPost);
        }
    }

    @Async
    public CompletableFuture<List<SentimentData>> analyzeSentimentBatch(List<SocialPost> socialPosts) {
        logger.info("Starting batch sentiment analysis for {} posts", socialPosts.size());
        
        if (!isInitialized) {
            logger.warn("Stanford CoreNLP not initialized, returning neutral sentiments for batch");
            List<SentimentData> fallbacks = socialPosts.stream()
                    .map(this::createFallbackSentiment)
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(fallbacks);
        }

        List<SentimentData> results = socialPosts.stream()
                .map(this::analyzeSentiment)
                .collect(Collectors.toList());
        
        logger.info("Completed batch sentiment analysis for {} posts", results.size());
        return CompletableFuture.completedFuture(results);
    }

    // Fixed method signature for controller
    public Map<String, Object> getHealthStatus() {
        if (isInitialized) {
            return Map.of(
                "service", "Stanford Sentiment Analysis",
                "status", "UP",
                "message", "Stanford CoreNLP initialized and ready",
                "timestamp", LocalDateTime.now()
            );
        } else {
            return Map.of(
                "service", "Stanford Sentiment Analysis",
                "status", "DOWN", 
                "message", "Stanford CoreNLP initialization failed - using fallback sentiment analysis",
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    // Added method for controller
    public Map<String, Object> getSentimentStatistics(Platform platform) {
        try {
            List<Object[]> distribution = sentimentDataRepository.getSentimentDistributionByPlatform(platform);
            Double averageSentiment = sentimentDataRepository.getAverageSentimentByPlatform(platform);
            
            Map<String, Long> sentimentCounts = distribution.stream()
                .collect(Collectors.toMap(
                    row -> ((SentimentLabel) row[0]).name(),
                    row -> (Long) row[1]
                ));
            
            long totalAnalyzed = sentimentCounts.values().stream().mapToLong(Long::longValue).sum();
            
            return Map.of(
                "platform", platform.name(),
                "totalAnalyzed", totalAnalyzed,
                "averageSentimentScore", averageSentiment != null ? averageSentiment : 0.0,
                "distribution", sentimentCounts,
                "lastUpdated", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error getting sentiment statistics for {}: {}", platform, e.getMessage());
            return Map.of(
                "platform", platform.name(),
                "error", "Unable to retrieve sentiment statistics",
                "totalAnalyzed", 0,
                "lastUpdated", LocalDateTime.now()
            );
        }
    }

    // Helper methods
    private SentimentData createFallbackSentiment(SocialPost socialPost) {
        SentimentData fallback = new SentimentData();
        fallback.setSocialPost(socialPost);
        fallback.setSentimentLabel(SentimentLabel.NEUTRAL);
        fallback.setSentimentScore(0.0);
        fallback.setConfidence(0.1);
        fallback.setProcessedAt(LocalDateTime.now());
        
        // Save fallback to database
        return sentimentDataRepository.save(fallback);
    }

    private double calculateConfidence(int positiveCount, int negativeCount, int neutralCount) {
        int total = positiveCount + negativeCount + neutralCount;
        if (total == 0) return 0.1;
        
        int maxCategory = Math.max(Math.max(positiveCount, negativeCount), neutralCount);
        return (double) maxCategory / total;
    }

    private SentimentLabel determineSentimentLabel(double score, double confidence) {
        if (confidence < 0.3) {
            return SentimentLabel.MIXED;
        }
        
        if (score > 0.2) return SentimentLabel.POSITIVE;
        if (score < -0.2) return SentimentLabel.NEGATIVE;
        return SentimentLabel.NEUTRAL;
    }

    private SentimentData createSentimentData(SocialPost socialPost, SentimentLabel label, 
                                            double score, double confidence) {
        SentimentData sentimentData = new SentimentData();
        sentimentData.setSocialPost(socialPost);
        sentimentData.setSentimentLabel(label);
        sentimentData.setSentimentScore(score);
        sentimentData.setConfidence(confidence);
        sentimentData.setProcessedAt(LocalDateTime.now());
        return sentimentData;
    }
}