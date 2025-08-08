package com.socialmedia.data.ingestion.repository;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Enhanced repository for SocialPost entities with advanced querying capabilities
 * for analytics, engagement metrics, and cross-platform analysis.
 */
@Repository
public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {
    
    // ===== BASIC QUERIES =====
    
    /**
     * Find posts by platform ordered by creation date (newest first)
     */
    List<SocialPost> findByPlatformOrderByCreatedAtDesc(Platform platform);
    
    /**
     * Find posts by platform with pagination
     */
    Page<SocialPost> findByPlatform(Platform platform, Pageable pageable);
    
    /**
     * Check if post exists by external ID and platform (duplicate detection)
     */
    boolean existsByExternalIdAndPlatform(String externalId, Platform platform);
    
    /**
     * Find post by external ID and platform
     */
    Optional<SocialPost> findByExternalIdAndPlatform(String externalId, Platform platform);
    
    /**
     * Find post by content hash and platform (content duplicate detection)
     */
    Optional<SocialPost> findByContentHashAndPlatform(String contentHash, Platform platform);
    
    // ===== TIME-BASED ANALYTICS =====
    
    /**
     * Count posts by platform since a specific date
     */
    @Query("SELECT COUNT(s) FROM SocialPost s WHERE s.platform = :platform AND s.createdAt > :since")
    Long countByPlatformSince(@Param("platform") Platform platform, @Param("since") LocalDateTime since);
    
    /**
     * Find posts created within a date range
     */
    @Query("SELECT s FROM SocialPost s WHERE s.createdAt BETWEEN :start AND :end ORDER BY s.createdAt DESC")
    List<SocialPost> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Get daily post counts for analytics dashboard
     */
    @Query("SELECT DATE(s.createdAt) as date, COUNT(s) as count " +
           "FROM SocialPost s " +
           "WHERE s.createdAt BETWEEN :start AND :end " +
           "GROUP BY DATE(s.createdAt) " +
           "ORDER BY date")
    List<Object[]> getDailyPostCounts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * Get hourly post distribution for trend analysis
     */
    @Query("SELECT HOUR(s.createdAt) as hour, COUNT(s) as count " +
           "FROM SocialPost s " +
           "WHERE s.platform = :platform AND s.createdAt > :since " +
           "GROUP BY HOUR(s.createdAt) " +
           "ORDER BY hour")
    List<Object[]> getHourlyDistribution(@Param("platform") Platform platform, @Param("since") LocalDateTime since);
    
    // ===== CONTENT ANALYSIS =====
    
    /**
     * Find posts containing specific keywords
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "(LOWER(s.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "s.platform = :platform " +
           "ORDER BY s.engagementScore DESC")
    List<SocialPost> findByContentContainingAndPlatform(@Param("keyword") String keyword, 
                                                        @Param("platform") Platform platform, 
                                                        Pageable pageable);
    
    /**
     * Find posts by author across platforms
     */
    @Query("SELECT s FROM SocialPost s WHERE s.author = :author ORDER BY s.createdAt DESC")
    List<SocialPost> findByAuthor(@Param("author") String author);
    
    /**
     * Find posts with specific hashtags
     */
    @Query("SELECT DISTINCT s FROM SocialPost s JOIN s.hashtags h WHERE h = :hashtag ORDER BY s.createdAt DESC")
    List<SocialPost> findByHashtag(@Param("hashtag") String hashtag);
    
    // ===== ENGAGEMENT ANALYTICS =====
    
    /**
     * Find high engagement posts above a threshold
     */
    @Query("SELECT s FROM SocialPost s WHERE s.engagementScore > :minScore ORDER BY s.engagementScore DESC")
    List<SocialPost> findHighEngagementPosts(@Param("minScore") Double minScore, Pageable pageable);
    
    /**
     * Get average engagement score by platform
     */
    @Query("SELECT AVG(s.engagementScore) FROM SocialPost s WHERE s.platform = :platform")
    Double getAverageEngagementByPlatform(@Param("platform") Platform platform);
    
    /**
     * Get engagement statistics for a platform
     */
    @Query("SELECT " +
           "AVG(s.engagementScore) as avgEngagement, " +
           "MAX(s.engagementScore) as maxEngagement, " +
           "MIN(s.engagementScore) as minEngagement, " +
           "COUNT(s) as totalPosts " +
           "FROM SocialPost s WHERE s.platform = :platform")
    Object[] getEngagementStats(@Param("platform") Platform platform);
    
    /**
     * Find trending posts (high engagement in recent time)
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "s.createdAt > :since AND " +
           "s.engagementScore > :minEngagement " +
           "ORDER BY s.engagementScore DESC, s.createdAt DESC")
    List<SocialPost> findTrendingPosts(@Param("since") LocalDateTime since, 
                                      @Param("minEngagement") Double minEngagement, 
                                      Pageable pageable);
    
    // ===== AUTHOR ANALYTICS =====
    
    /**
     * Get top authors by post count for a platform
     */
    @Query("SELECT s.author, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.platform = :platform " +
           "GROUP BY s.author " +
           "HAVING COUNT(s) > :minPosts " +
           "ORDER BY avgEngagement DESC")
    List<Object[]> getTopAuthorsByEngagement(@Param("platform") Platform platform, @Param("minPosts") Long minPosts);
    
    /**
     * Get author posting frequency
     */
    @Query("SELECT s.author, COUNT(s) as postCount " +
           "FROM SocialPost s " +
           "WHERE s.platform = :platform AND s.createdAt > :since " +
           "GROUP BY s.author " +
           "ORDER BY postCount DESC")
    List<Object[]> getAuthorPostingFrequency(@Param("platform") Platform platform, @Param("since") LocalDateTime since);
    
    // ===== PLATFORM-SPECIFIC QUERIES =====
    
    /**
     * Reddit-specific: Find posts by subreddit
     */
    @Query("SELECT s FROM SocialPost s WHERE s.subreddit = :subreddit ORDER BY s.engagementScore DESC")
    List<SocialPost> findBySubreddit(@Param("subreddit") String subreddit, Pageable pageable);
    
    /**
     * Reddit-specific: Get subreddit statistics
     */
    @Query("SELECT s.subreddit, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.platform = 'REDDIT' AND s.subreddit IS NOT NULL " +
           "GROUP BY s.subreddit " +
           "ORDER BY avgEngagement DESC")
    List<Object[]> getSubredditStats();
    
    /**
     * Find posts with high upvote ratio (Reddit)
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "s.platform = 'REDDIT' AND " +
           "s.upvotes > 0 AND s.downvotes IS NOT NULL AND " +
           "(s.upvotes * 1.0 / (s.upvotes + s.downvotes)) > :ratio " +
           "ORDER BY s.upvotes DESC")
    List<SocialPost> findHighUpvoteRatioPosts(@Param("ratio") Double ratio, Pageable pageable);
    
    // ===== BATCH OPERATIONS =====
    
    /**
     * Update engagement score for a specific post
     */
    @Modifying
    @Transactional
    @Query("UPDATE SocialPost s SET s.engagementScore = :score WHERE s.id = :id")
    int updateEngagementScore(@Param("id") Long id, @Param("score") Double score);
    
    /**
     * Mark posts as processed
     */
    @Modifying
    @Transactional
    @Query("UPDATE SocialPost s SET s.processedAt = CURRENT_TIMESTAMP WHERE s.id IN :ids")
    int markAsProcessed(@Param("ids") List<Long> ids);
    
    /**
     * Bulk update processed timestamp for posts without sentiment analysis
     */
    @Modifying
    @Transactional
    @Query("UPDATE SocialPost s SET s.processedAt = CURRENT_TIMESTAMP " +
           "WHERE s.processedAt IS NULL AND s.sentimentData IS NULL")
    int markUnprocessedForSentimentAnalysis();
    
    // ===== COMPLEX ANALYTICS =====
    
    /**
     * Get platform comparison data
     */
    @Query("SELECT s.platform, " +
           "COUNT(s) as postCount, " +
           "AVG(s.engagementScore) as avgEngagement, " +
           "SUM(CASE WHEN s.engagementScore > 100 THEN 1 ELSE 0 END) as highEngagementCount " +
           "FROM SocialPost s " +
           "WHERE s.createdAt > :since " +
           "GROUP BY s.platform")
    List<Object[]> getPlatformComparisonData(@Param("since") LocalDateTime since);
    
    /**
     * Find posts that need sentiment analysis
     */
    @Query("SELECT s FROM SocialPost s WHERE s.sentimentData IS NULL AND s.content IS NOT NULL ORDER BY s.createdAt DESC")
    List<SocialPost> findPostsNeedingSentimentAnalysis(Pageable pageable);
    
    /**
     * Get engagement trends over time
     */
    @Query("SELECT DATE(s.createdAt) as date, " +
           "AVG(s.engagementScore) as avgEngagement, " +
           "COUNT(s) as postCount " +
           "FROM SocialPost s " +
           "WHERE s.platform = :platform AND s.createdAt BETWEEN :start AND :end " +
           "GROUP BY DATE(s.createdAt) " +
           "ORDER BY date")
    List<Object[]> getEngagementTrends(@Param("platform") Platform platform, 
                                      @Param("start") LocalDateTime start, 
                                      @Param("end") LocalDateTime end);
    
    /**
     * Find similar posts by content hash (potential duplicates)
     */
    @Query("SELECT s FROM SocialPost s WHERE s.contentHash = :hash AND s.id != :excludeId")
    List<SocialPost> findSimilarPosts(@Param("hash") String contentHash, @Param("excludeId") Long excludeId);
    
    // ===== STATISTICAL QUERIES =====
    
    /**
     * Get posting volume statistics
     */
    @Query("SELECT " +
           "COUNT(s) as totalPosts, " +
           "COUNT(DISTINCT s.author) as uniqueAuthors, " +
           "COUNT(DISTINCT s.platform) as platformsUsed, " +
           "AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.createdAt > :since")
    Object[] getVolumeStatistics(@Param("since") LocalDateTime since);
}