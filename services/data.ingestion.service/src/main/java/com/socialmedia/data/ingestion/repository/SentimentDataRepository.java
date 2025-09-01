package com.socialmedia.data.ingestion.repository;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SentimentData;
import com.socialmedia.data.ingestion.model.SentimentLabel;
import com.socialmedia.data.ingestion.model.SocialPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Enhanced sentiment repository for MVP with additional controller support methods
 */
@Repository
public interface SentimentDataRepository extends JpaRepository<SentimentData, Long> {
    
    // ===== EXISTING METHODS (KEEP ALL YOUR CURRENT METHODS) =====
    
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
    
    // ===== NEW METHODS FOR CONTROLLER SUPPORT =====
    
    /**
     * Count by sentiment label (for stats endpoint)
     */
    long countBySentimentLabel(SentimentLabel sentimentLabel);
    
    /**
     * Count recent sentiments (for stats endpoint)
     */
    long countByProcessedAtAfter(LocalDateTime date);
    
    /**
     * Get sentiment breakdown by platform (for breakdown endpoint)
     * Returns: platform, sentiment_label, count
     */
    @Query("SELECT sp.platform, sd.sentimentLabel, COUNT(sd) " +
           "FROM SentimentData sd JOIN sd.socialPost sp " +
           "GROUP BY sp.platform, sd.sentimentLabel " +
           "ORDER BY sp.platform, sd.sentimentLabel")
    List<Object[]> getSentimentBreakdown();
    
    /**
     * Find posts without sentiment analysis (for processing endpoint)
     */
    @Query("SELECT sp FROM SocialPost sp WHERE sp.id NOT IN " +
           "(SELECT sd.socialPost.id FROM SentimentData sd WHERE sd.socialPost.id IS NOT NULL)")
    List<SocialPost> findPostsWithoutSentiment();
    
    /**
     * Get recent sentiment trends (last 7 days by day)
     */
    @Query("SELECT DATE(sd.processedAt) as processDate, " +
           "sd.sentimentLabel, " +
           "COUNT(sd) as dailyCount " +
           "FROM SentimentData sd " +
           "WHERE sd.processedAt >= :since " +
           "GROUP BY DATE(sd.processedAt), sd.sentimentLabel " +
           "ORDER BY processDate DESC, sd.sentimentLabel")
    List<Object[]> getRecentSentimentTrends(@Param("since") LocalDateTime since);
    
    /**
     * Get top positive/negative posts by sentiment score
     */
    @Query("SELECT sp.title, sp.externalId, sp.platform, sd.sentimentScore, sd.sentimentLabel " +
           "FROM SentimentData sd JOIN sd.socialPost sp " +
           "WHERE sd.sentimentLabel = :label " +
           "ORDER BY sd.sentimentScore DESC")
    List<Object[]> getTopPostsBySentiment(@Param("label") SentimentLabel label);
}