package com.socialmedia.data.ingestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entity representing sentiment analysis data for a social media post
 */
@Entity
@Table(name = "sentiment_data")
public class SentimentData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_post_id", nullable = false)
    private SocialPost socialPost;
    
    @NotNull(message = "Sentiment score is required")
    @DecimalMin(value = "-1.0", message = "Sentiment score must be between -1.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Sentiment score must be between -1.0 and 1.0")
    @Column(name = "sentiment_score", nullable = false)
    private Double sentimentScore; // -1.0 (negative) to 1.0 (positive)
    
    @NotNull(message = "Sentiment label is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_label", nullable = false)
    private SentimentLabel sentimentLabel;
    
    @DecimalMin(value = "0.0", message = "Confidence must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0.0 and 1.0")
    @Column(name = "confidence")
    private Double confidence; // 0.0 to 1.0
    
    @Column(name = "positive_probability")
    private Double positiveProbability;
    
    @Column(name = "negative_probability")
    private Double negativeProbability;
    
    @Column(name = "neutral_probability")
    private Double neutralProbability;
    
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
    
    @Column(name = "algorithm_version")
    private String algorithmVersion;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    // Enum for sentiment labels
    public enum SentimentLabel {
        POSITIVE,
        NEGATIVE, 
        NEUTRAL
    }
    
    // Constructors
    public SentimentData() {
        this.analyzedAt = LocalDateTime.now();
    }
    
    public SentimentData(SocialPost socialPost, Double sentimentScore, SentimentLabel sentimentLabel, Double confidence) {
        this();
        this.socialPost = socialPost;
        this.sentimentScore = sentimentScore;
        this.sentimentLabel = sentimentLabel;
        this.confidence = confidence;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public SocialPost getSocialPost() {
        return socialPost;
    }
    
    public void setSocialPost(SocialPost socialPost) {
        this.socialPost = socialPost;
    }
    
    public Double getSentimentScore() {
        return sentimentScore;
    }
    
    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
    
    public SentimentLabel getSentimentLabel() {
        return sentimentLabel;
    }
    
    public void setSentimentLabel(SentimentLabel sentimentLabel) {
        this.sentimentLabel = sentimentLabel;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public Double getPositiveProbability() {
        return positiveProbability;
    }
    
    public void setPositiveProbability(Double positiveProbability) {
        this.positiveProbability = positiveProbability;
    }
    
    public Double getNegativeProbability() {
        return negativeProbability;
    }
    
    public void setNegativeProbability(Double negativeProbability) {
        this.negativeProbability = negativeProbability;
    }
    
    public Double getNeutralProbability() {
        return neutralProbability;
    }
    
    public void setNeutralProbability(Double neutralProbability) {
        this.neutralProbability = neutralProbability;
    }
    
    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
    
    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
    
    public String getAlgorithmVersion() {
        return algorithmVersion;
    }
    
    public void setAlgorithmVersion(String algorithmVersion) {
        this.algorithmVersion = algorithmVersion;
    }
    
    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    @Override
    public String toString() {
        return "SentimentData{" +
                "id=" + id +
                ", sentimentScore=" + sentimentScore +
                ", sentimentLabel=" + sentimentLabel +
                ", confidence=" + confidence +
                ", analyzedAt=" + analyzedAt +
                '}';
    }
}