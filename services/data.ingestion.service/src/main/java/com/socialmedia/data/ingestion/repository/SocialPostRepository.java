package com.socialmedia.data.ingestion.repository;

import com.socialmedia.data.ingestion.model.SocialPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {
    
    /**
     * Find post by external ID and platform to avoid duplicates
     */
    Optional<SocialPost> findByPostIdAndPlatform(String postId, String platform);
    
    /**
     * Check if post exists to avoid duplicates during ingestion
     */
    boolean existsByPostIdAndPlatform(String postId, String platform);
    
    /**
     * Find posts by platform for platform-specific queries
     */
    List<SocialPost> findByPlatform(String platform);
    
    /**
     * Count posts by platform for statistics
     */
    long countByPlatform(String platform);
    
    /**
     * Find recent posts for monitoring
     */
    List<SocialPost> findByIngestionTimeAfter(LocalDateTime time);
    
    /**
     * Count recent posts for statistics
     */
    long countByIngestionTimeAfter(LocalDateTime time);
    
    /**
     * Find posts that haven't been processed for sentiment analysis yet
     */
    List<SocialPost> findByProcessedAtIsNull();
    
    /**
     * Find posts by subreddit for Reddit-specific analysis
     */
    List<SocialPost> findBySubreddit(String subreddit);
    
    /**
     * Find posts by date range for batch processing
     */
    @Query("SELECT sp FROM SocialPost sp WHERE sp.createdAt BETWEEN :startDate AND :endDate ORDER BY sp.createdAt DESC")
    List<SocialPost> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find top posts by score for trending analysis
     */
    @Query("SELECT sp FROM SocialPost sp WHERE sp.platform = :platform AND sp.score IS NOT NULL ORDER BY sp.score DESC")
    List<SocialPost> findTopPostsByScore(@Param("platform") String platform, 
                                       org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find posts for sentiment analysis queue
     */
    @Query("SELECT sp FROM SocialPost sp WHERE sp.processedAt IS NULL AND sp.content IS NOT NULL ORDER BY sp.ingestionTime ASC")
    List<SocialPost> findUnprocessedPostsForSentimentAnalysis(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get ingestion statistics by platform and time range
     */
    @Query("SELECT sp.platform, COUNT(sp), AVG(sp.score) FROM SocialPost sp WHERE sp.ingestionTime >= :since GROUP BY sp.platform")
    List<Object[]> getIngestionStatistics(@Param("since") LocalDateTime since);
    
    /**
     * Delete old posts for data retention (if needed)
     */
    void deleteByIngestionTimeBefore(LocalDateTime cutoffTime);
}