package com.socialmedia.data.ingestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing sentiment analysis results for social media posts.
 * Separated from SocialPost to allow for independent sentiment processing
 * and potential re-analysis with different algorithms.
 */
@Entity
@Table(name = "sentiment_data", 
       indexes = {
           @Index(name = "idx_sentiment_score", columnList = "overallSentiment"),
           @Index(name = "idx_sentiment_label", columnList = "sentimentLabel"),
           @Index(name = "idx_processed_at", columnList = "processedAt"),
           @Index(name = "idx_confidence_score", columnList = "confidenceScore")
       })
public class SentimentData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "social_post_id", nullable = false, unique = true)
    @NotNull(message = "Social post cannot be null")
    private SocialPost socialPost;
    
    @Column(name = "overall_sentiment", nullable = false)
    @NotNull(message = "Overall sentiment score cannot be null")
    @DecimalMin(value = "-1.0", message = "Sentiment score must be between -1.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Sentiment score must be between -1.0 and 1.0")
    private Double overallSentiment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_label", nullable = false, length = 20)
    @NotNull(message = "Sentiment label cannot be null")
    private SentimentLabel sentimentLabel;
    
    @Column(name = "confidence_score")
    @DecimalMin(value = "0.0", message = "Confidence score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be between 0.0 and 1.0")
    private Double confidenceScore;
    
    @Column(name = "positive_score")
    @DecimalMin(value = "0.0", message = "Positive score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Positive score must be between 0.0 and 1.0")
    private Double positiveScore;
    
    @Column(name = "negative_score")
    @DecimalMin(value = "0.0", message = "Negative score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Negative score must be between 0.0 and 1.0")
    private Double negativeScore;
    
    @Column(name = "neutral_score")
    @DecimalMin(value = "0.0", message = "Neutral score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Neutral score must be between 0.0 and 1.0")
    private Double neutralScore;
    
    @Column(name = "processed_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime processedAt;
    
    @Column(name = "processing_version", length = 50)
    private String processingVersion = "1.0";
    
    @Column(name = "algorithm_used", length = 100)
    private String algorithmUsed;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "language_detected", length = 10)
    private String languageDetected;
    
    @Column(name = "emotion_detected", length = 50)
    private String emotionDetected;
    
    // Constructors
    public SentimentData() {}
    
    public SentimentData(SocialPost socialPost, Double overallSentiment, SentimentLabel sentimentLabel) {
        this.socialPost = socialPost;
        this.overallSentiment = overallSentiment;
        this.sentimentLabel = sentimentLabel;
        this.processedAt = LocalDateTime.now();
    }
    
    // Business methods
    @PrePersist
    @PreUpdate
    private void validateScores() {
        // Ensure scores add up to approximately 1.0 if all are provided
        if (positiveScore != null && negativeScore != null && neutralScore != null) {
            double sum = positiveScore + negativeScore + neutralScore;
            if (Math.abs(sum - 1.0) > 0.01) {
                throw new IllegalStateException("Positive, negative, and neutral scores must sum to 1.0");
            }
        }
        
        // Auto-determine sentiment label if not provided
        if (sentimentLabel == null && overallSentiment != null) {
            this.sentimentLabel = determineSentimentLabel(overallSentiment);
        }
    }
    
    private SentimentLabel determineSentimentLabel(double sentiment) {
        if (sentiment > 0.1) return SentimentLabel.POSITIVE;
        if (sentiment < -0.1) return SentimentLabel.NEGATIVE;
        return SentimentLabel.NEUTRAL;
    }
    
    /**
     * Check if sentiment analysis is reliable based on confidence score
     */
    public boolean isReliable() {
        return confidenceScore != null && confidenceScore >= 0.7;
    }
    
    /**
     * Check if sentiment is strongly positive or negative
     */
    public boolean isStrongSentiment() {
        return Math.abs(overallSentiment) > 0.5;
    }
    
    /**
     * Get the dominant emotion category
     */
    public SentimentLabel getDominantSentiment() {
        if (positiveScore == null || negativeScore == null || neutralScore == null) {
            return sentimentLabel;
        }
        
        double max = Math.max(positiveScore, Math.max(negativeScore, neutralScore));
        if (max == positiveScore) return SentimentLabel.POSITIVE;
        if (max == negativeScore) return SentimentLabel.NEGATIVE;
        return SentimentLabel.NEUTRAL;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SocialPost getSocialPost() { return socialPost; }
    public void setSocialPost(SocialPost socialPost) { this.socialPost = socialPost; }
    
    public Double getOverallSentiment() { return overallSentiment; }
    public void setOverallSentiment(Double overallSentiment) { this.overallSentiment = overallSentiment; }
    
    public SentimentLabel getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(SentimentLabel sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public Double getPositiveScore() { return positiveScore; }
    public void setPositiveScore(Double positiveScore) { this.positiveScore = positiveScore; }
    
    public Double getNegativeScore() { return negativeScore; }
    public void setNegativeScore(Double negativeScore) { this.negativeScore = negativeScore; }
    
    public Double getNeutralScore() { return neutralScore; }
    public void setNeutralScore(Double neutralScore) { this.neutralScore = neutralScore; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public String getProcessingVersion() { return processingVersion; }
    public void setProcessingVersion(String processingVersion) { this.processingVersion = processingVersion; }
    
    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getLanguageDetected() { return languageDetected; }
    public void setLanguageDetected(String languageDetected) { this.languageDetected = languageDetected; }
    
    public String getEmotionDetected() { return emotionDetected; }
    public void setEmotionDetected(String emotionDetected) { this.emotionDetected = emotionDetected; }
    
    // Legacy compatibility methods for your existing code
    @Deprecated
    public String getSentimentLabelString() {
        return sentimentLabel != null ? sentimentLabel.name() : null;
    }
    
    @Deprecated
    public void setSentimentLabelString(String sentimentLabel) {
        if (sentimentLabel != null) {
            this.sentimentLabel = SentimentLabel.valueOf(sentimentLabel.toUpperCase());
        }
    }
    
    @Deprecated
    public Double getSentimentScore() { return overallSentiment; }
    @Deprecated
    public void setSentimentScore(Double sentimentScore) { this.overallSentiment = sentimentScore; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SentimentData that = (SentimentData) o;
        return Objects.equals(socialPost, that.socialPost);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(socialPost);
    }
    
    @Override
    public String toString() {
        return "SentimentData{" +
                "id=" + id +
                ", overallSentiment=" + overallSentiment +
                ", sentimentLabel=" + sentimentLabel +
                ", confidenceScore=" + confidenceScore +
                ", processedAt=" + processedAt +
                '}';
    }
}