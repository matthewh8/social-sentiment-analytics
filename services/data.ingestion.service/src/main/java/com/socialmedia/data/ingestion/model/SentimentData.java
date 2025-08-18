package com.socialmedia.data.ingestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Sentiment data analysis
 */
@Entity
@Table(name = "sentiment_data", 
       indexes = {
           @Index(name = "idx_sentiment_label", columnList = "sentimentLabel"),
           @Index(name = "idx_sentiment_score", columnList = "sentimentScore")
       })
public class SentimentData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "social_post_id", nullable = false, unique = true)
    @NotNull(message = "Social post cannot be null")
    private SocialPost socialPost;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_label", nullable = false, length = 20)
    @NotNull(message = "Sentiment label cannot be null")
    private SentimentLabel sentimentLabel;
    
    @Column(name = "sentiment_score", nullable = false)
    @NotNull(message = "Sentiment score cannot be null")
    @DecimalMin(value = "0.0", message = "Sentiment score must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Sentiment score must be between 0.0 and 1.0")
    private Double sentimentScore;
    
    @Column(name = "confidence", nullable = false)
    @NotNull(message = "Confidence cannot be null")
    @DecimalMin(value = "0.0", message = "Confidence must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0.0 and 1.0")
    private Double confidence;
    
    @Column(name = "processed_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime processedAt;
    
    // Constructors
    public SentimentData() {}
    
    public SentimentData(SocialPost socialPost, SentimentLabel sentimentLabel, Double sentimentScore, Double confidence) {
        this.socialPost = socialPost;
        this.sentimentLabel = sentimentLabel;
        this.sentimentScore = sentimentScore;
        this.confidence = confidence;
        this.processedAt = LocalDateTime.now();
    }
    
    // Simple getters and setters (no business logic)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SocialPost getSocialPost() { return socialPost; }
    public void setSocialPost(SocialPost socialPost) { this.socialPost = socialPost; }
    
    public SentimentLabel getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(SentimentLabel sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    
    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
    
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
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
                ", sentimentLabel=" + sentimentLabel +
                ", sentimentScore=" + sentimentScore +
                ", confidence=" + confidence +
                '}';
    }
}