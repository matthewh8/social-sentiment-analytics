package com.socialmedia.data.ingestion.repository;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SentimentData entities with analytics capabilities
 * for sentiment trends, platform comparisons, and author sentiment profiles.
 */
@Repository
public interface SentimentDataRepository extends JpaRepository<SentimentData, Long> {
    
    // ===== BASIC QUERIES =====
    
    /**
     * Find sentiment data by social post ID
     */
    Optional<SentimentData> findBySocialPostId(Long socialPostId);
    
    /**
     * Find sentiment data by post external ID and platform
     */
    @Query("SELECT sd FROM SentimentData sd JOIN sd.socialPost sp " +
           "WHERE sp.externalId = :externalId AND sp.platform = :platform")
    Optional<SentimentData> findByPostExternalIdAndPlatform(@Param("externalId") String externalId, 
                                                           @Param("platform") Platform platform);
    
    // ===== PLATFORM SENTIMENT ANALYTICS =====
    
    /**
     * Get average sentiment by platform
     */
    @Query("SELECT AVG(sd.overallSentiment) FROM SentimentData sd " +
           "JOIN sd.socialPost sp WHERE sp.platform = :platform")
    Double getAverageSentimentByPlatform(@Param("platform") Platform platform);
    
    /**
     * Get sentiment distribution for a platform
     */
    @Query("SELECT sd.sentimentLabel, COUNT(sd) FROM SentimentData sd " +
           "JOIN sd.socialPost sp WHERE sp.platform = :platform " +
           "GROUP BY sd.sentimentLabel")
    List<Object[]> getSentimentDistributionByPlatform(@Param("platform") Platform platform);
    
    /**
     * Get sentiment statistics for a platform
     */
    @Query("SELECT " +
           "AVG(sd.overallSentiment) as avgSentiment, " +
           "MAX(sd.overallSentiment) as maxSentiment, " +
           "MIN(sd.overallSentiment) as minSentiment, " +
           "AVG(sd.confidenceScore) as avgConfidence, " +
           "COUNT(sd) as totalAnalyzed " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp WHERE sp.platform = :platform")
    Object[] getSentimentStatsByPlatform(@Param("platform") Platform platform);
    
    // ===== TIME-BASED SENTIMENT ANALYSIS =====
    
    /**
     * Get sentiment trends over time for a platform
     */
    @Query("SELECT DATE(sd.processedAt) as date, " +
           "AVG(sd.overallSentiment) as avgSentiment, " +
           "COUNT(sd) as count " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = :platform AND sd.processedAt BETWEEN :start AND :end " +
           "GROUP BY DATE(sd.processedAt) " +
           "ORDER BY date")
    List<Object[]> getSentimentTrends(@Param("platform") Platform platform,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
    
    /**
     * Find posts with extreme sentiment (very positive or negative)
     */
    @Query("SELECT sd FROM SentimentData sd " +
           "WHERE ABS(sd.overallSentiment) > :threshold " +
           "ORDER BY ABS(sd.overallSentiment) DESC")
    List<SentimentData> findExtremeSentiment(@Param("threshold") Double threshold, Pageable pageable);
    
    /**
     * Get hourly sentiment distribution
     */
    @Query("SELECT HOUR(sp.createdAt) as hour, AVG(sd.overallSentiment) as avgSentiment " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = :platform AND sp.createdAt > :since " +
           "GROUP BY HOUR(sp.createdAt) " +
           "ORDER BY hour")
    List<Object[]> getHourlySentimentDistribution(@Param("platform") Platform platform, 
                                                 @Param("since") LocalDateTime since);
    
    // ===== AUTHOR SENTIMENT PROFILES =====
    
    /**
     * Get author sentiment profiles
     */
    @Query("SELECT sp.author, " +
           "AVG(sd.overallSentiment) as avgSentiment, " +
           "COUNT(sd) as postCount, " +
           "AVG(sd.confidenceScore) as avgConfidence " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "GROUP BY sp.author " +
           "HAVING COUNT(sd) > :minPosts " +
           "ORDER BY avgSentiment DESC")
    List<Object[]> getAuthorSentimentProfiles(@Param("minPosts") Long minPosts);
    
    /**
     * Find most positive authors by platform
     */
    @Query("SELECT sp.author, AVG(sd.overallSentiment) as avgSentiment, COUNT(sd) as postCount " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = :platform " +
           "GROUP BY sp.author " +
           "HAVING COUNT(sd) > :minPosts AND AVG(sd.overallSentiment) > 0.3 " +
           "ORDER BY avgSentiment DESC")
    List<Object[]> getMostPositiveAuthors(@Param("platform") Platform platform, @Param("minPosts") Long minPosts);
    
    /**
     * Find most negative authors by platform
     */
    @Query("SELECT sp.author, AVG(sd.overallSentiment) as avgSentiment, COUNT(sd) as postCount " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = :platform " +
           "GROUP BY sp.author " +
           "HAVING COUNT(sd) > :minPosts AND AVG(sd.overallSentiment) < -0.3 " +
           "ORDER BY avgSentiment ASC")
    List<Object[]> getMostNegativeAuthors(@Param("platform") Platform platform, @Param("minPosts") Long minPosts);
    
    // ===== CONTENT SENTIMENT ANALYSIS =====
    
    /**
     * Find sentiment by keyword in content
     */
    @Query("SELECT AVG(sd.overallSentiment), COUNT(sd) " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE LOWER(sp.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Object[] getSentimentForKeyword(@Param("keyword") String keyword);
    
    /**
     * Find posts with specific sentiment label and high confidence
     */
    @Query("SELECT sd FROM SentimentData sd " +
           "WHERE sd.sentimentLabel = :label AND sd.confidenceScore > :minConfidence " +
           "ORDER BY sd.confidenceScore DESC")
    List<SentimentData> findHighConfidenceSentiment(@Param("label") SentimentLabel label, 
                                                   @Param("minConfidence") Double minConfidence, 
                                                   Pageable pageable);
    
    // ===== QUALITY METRICS =====
    
    /**
     * Get sentiment analysis quality metrics
     */
    @Query("SELECT " +
           "AVG(sd.confidenceScore) as avgConfidence, " +
           "COUNT(CASE WHEN sd.confidenceScore > 0.8 THEN 1 END) as highConfidenceCount, " +
           "COUNT(CASE WHEN sd.confidenceScore < 0.5 THEN 1 END) as lowConfidenceCount, " +
           "COUNT(sd) as totalAnalyzed " +
           "FROM SentimentData sd")
    Object[] getQualityMetrics();
    
    /**
     * Find posts that need re-analysis (low confidence)
     */
    @Query("SELECT sd FROM SentimentData sd " +
           "WHERE sd.confidenceScore < :threshold " +
           "ORDER BY sd.processedAt ASC")
    List<SentimentData> findLowConfidenceAnalysis(@Param("threshold") Double threshold, Pageable pageable);
    
    // ===== CROSS-PLATFORM COMPARISONS =====
    
    /**
     * Compare sentiment across platforms
     */
    @Query("SELECT sp.platform, " +
           "AVG(sd.overallSentiment) as avgSentiment, " +
           "COUNT(sd) as count, " +
           "SUM(CASE WHEN sd.sentimentLabel = 'POSITIVE' THEN 1 ELSE 0 END) as positiveCount, " +
           "SUM(CASE WHEN sd.sentimentLabel = 'NEGATIVE' THEN 1 ELSE 0 END) as negativeCount " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sd.processedAt > :since " +
           "GROUP BY sp.platform")
    List<Object[]> getCrossPlatformSentimentComparison(@Param("since") LocalDateTime since);
    
    // ===== REDDIT-SPECIFIC SENTIMENT ANALYSIS =====
    
    /**
     * Get sentiment by subreddit
     */
    @Query("SELECT sp.subreddit, " +
           "AVG(sd.overallSentiment) as avgSentiment, " +
           "COUNT(sd) as postCount " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = 'REDDIT' AND sp.subreddit IS NOT NULL " +
           "GROUP BY sp.subreddit " +
           "HAVING COUNT(sd) > :minPosts " +
           "ORDER BY avgSentiment DESC")
    List<Object[]> getSentimentBySubreddit(@Param("minPosts") Long minPosts);
    
    // ===== ENGAGEMENT vs SENTIMENT CORRELATION =====
    
    /**
     * Analyze correlation between sentiment and engagement
     */
    @Query("SELECT sd.sentimentLabel, " +
           "AVG(sp.engagementScore) as avgEngagement, " +
           "COUNT(sd) as count " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = :platform " +
           "GROUP BY sd.sentimentLabel")
    List<Object[]> getSentimentEngagementCorrelation(@Param("platform") Platform platform);
    
    /**
     * Find high engagement posts with specific sentiment
     */
    @Query("SELECT sd FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sd.sentimentLabel = :sentiment AND sp.engagementScore > :minEngagement " +
           "ORDER BY sp.engagementScore DESC")
    List<SentimentData> findHighEngagementBySentiment(@Param("sentiment") SentimentLabel sentiment,
                                                     @Param("minEngagement") Double minEngagement,
                                                     Pageable pageable);
    
    // ===== PROCESSING STATISTICS =====
    
    /**
     * Get processing performance metrics
     */
    @Query("SELECT " +
           "AVG(sd.processingTimeMs) as avgProcessingTime, " +
           "MAX(sd.processingTimeMs) as maxProcessingTime, " +
           "COUNT(sd) as totalProcessed, " +
           "sd.algorithmUsed " +
           "FROM SentimentData sd " +
           "WHERE sd.processedAt > :since " +
           "GROUP BY sd.algorithmUsed")
    List<Object[]> getProcessingPerformanceMetrics(@Param("since") LocalDateTime since);
}