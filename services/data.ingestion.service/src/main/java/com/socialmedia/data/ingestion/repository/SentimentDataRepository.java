package com.socialmedia.data.ingestion.repository;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Simplified sentiment repository for MVP
 * Removed: complex analytics, author profiles, processing metrics
 * Kept: basic sentiment queries, platform comparisons
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
     * Get sentiment distribution for a platform
     */
    @Query("SELECT sd.sentimentLabel, COUNT(sd) FROM SentimentData sd " +
           "JOIN sd.socialPost sp WHERE sp.platform = :platform " +
           "GROUP BY sd.sentimentLabel")
    List<Object[]> getSentimentDistributionByPlatform(@Param("platform") Platform platform);
    
    /**
     * Get average sentiment score by platform
     */
    @Query("SELECT AVG(sd.sentimentScore) FROM SentimentData sd " +
           "JOIN sd.socialPost sp WHERE sp.platform = :platform")
    Double getAverageSentimentByPlatform(@Param("platform") Platform platform);
    
    // ===== CROSS-PLATFORM COMPARISON =====
    
    /**
     * Compare sentiment across platforms (simplified)
     */
    @Query("SELECT sp.platform, " +
           "AVG(sd.sentimentScore) as avgSentiment, " +
           "COUNT(sd) as count, " +
           "SUM(CASE WHEN sd.sentimentLabel = 'POSITIVE' THEN 1 ELSE 0 END) as positiveCount, " +
           "SUM(CASE WHEN sd.sentimentLabel = 'NEGATIVE' THEN 1 ELSE 0 END) as negativeCount " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sd.processedAt > :since " +
           "GROUP BY sp.platform")
    List<Object[]> getCrossPlatformSentimentComparison(@Param("since") LocalDateTime since);
    
    // ===== REDDIT-SPECIFIC =====
    
    /**
     * Get sentiment by subreddit
     */
    @Query("SELECT sp.subreddit, " +
           "AVG(sd.sentimentScore) as avgSentiment, " +
           "COUNT(sd) as postCount " +
           "FROM SentimentData sd " +
           "JOIN sd.socialPost sp " +
           "WHERE sp.platform = 'REDDIT' AND sp.subreddit IS NOT NULL " +
           "GROUP BY sp.subreddit " +
           "HAVING COUNT(sd) > :minPosts " +
           "ORDER BY avgSentiment DESC")
    List<Object[]> getSentimentBySubreddit(@Param("minPosts") Long minPosts);
    
    // ===== ENGAGEMENT vs SENTIMENT =====
    
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
}