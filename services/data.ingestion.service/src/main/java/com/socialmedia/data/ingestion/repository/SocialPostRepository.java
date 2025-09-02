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
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SocialPost entities - Reddit and YouTube focused (2025 version)
 * Clean MVP version without Twitter, with consistent title handling
 */
@Repository
public interface SocialPostRepository extends JpaRepository<SocialPost, Long> {
    
    // ===== BASIC CRUD & DUPLICATE DETECTION =====
    
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
    
    /**
     * Find similar posts by content hash (potential duplicates)
     */
    @Query("SELECT s FROM SocialPost s WHERE s.contentHash = :hash AND s.id != :excludeId")
    List<SocialPost> findSimilarPosts(@Param("hash") String contentHash, @Param("excludeId") Long excludeId);
    
    // ===== PLATFORM & TIME-BASED QUERIES =====
    
    /**
     * Find posts by platform with pagination
     */
    Page<SocialPost> findByPlatform(Platform platform, Pageable pageable);
    
    /**
     * Find posts by multiple platforms with pagination
     */
    Page<SocialPost> findByPlatformIn(List<Platform> platforms, Pageable pageable);
    
    /**
     * Find posts by platform and created after a specific date
     */
    List<SocialPost> findByPlatformAndCreatedAtAfter(Platform platform, LocalDateTime date);
    
    /**
     * Find posts by platform and created between dates
     */
    List<SocialPost> findByPlatformAndCreatedAtBetween(Platform platform, 
                                                      LocalDateTime startDate, 
                                                      LocalDateTime endDate);
    
    /**
     * Find posts by platforms and created between dates with pagination
     */
    Page<SocialPost> findByPlatformInAndCreatedAtBetween(List<Platform> platforms, 
                                                        LocalDateTime startDate, 
                                                        LocalDateTime endDate, 
                                                        Pageable pageable);
    
    /**
     * Find posts created between dates with pagination
     */
    Page<SocialPost> findByCreatedAtBetween(LocalDateTime startDate, 
                                          LocalDateTime endDate, 
                                          Pageable pageable);
    
    // ===== COUNT & AGGREGATION QUERIES =====
    
    /**
     * Count posts by platform since a specific date
     */
    @Query("SELECT COUNT(s) FROM SocialPost s WHERE s.platform = :platform AND s.createdAt > :since")
    Long countByPlatformSince(@Param("platform") Platform platform, @Param("since") LocalDateTime since);
    
    /**
     * Count posts created between dates
     */
    Long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Count distinct authors between dates
     */
    @Query("SELECT COUNT(DISTINCT s.author) FROM SocialPost s WHERE s.createdAt BETWEEN :start AND :end")
    Long countDistinctAuthorsByCreatedAtBetween(@Param("start") LocalDateTime startDate, 
                                               @Param("end") LocalDateTime endDate);
    
    /**
     * Average engagement score between dates
     */
    @Query("SELECT AVG(s.engagementScore) FROM SocialPost s WHERE s.createdAt BETWEEN :start AND :end")
    Double averageEngagementScoreByCreatedAtBetween(@Param("start") LocalDateTime startDate, 
                                                    @Param("end") LocalDateTime endDate);
    
    // ===== CONTENT SEARCH & FILTERING =====
    
    /**
     * Find posts containing keywords in title or content (both platforms have titles)
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "(LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
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
    
    // ===== REDDIT-SPECIFIC QUERIES =====
    
    /**
     * Find posts by subreddit
     */
    List<SocialPost> findBySubreddit(String subreddit, Pageable pageable);
    
    /**
     * Get subreddit statistics (Reddit only)
     */
    @Query("SELECT s.subreddit, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.platform = 'REDDIT' AND s.subreddit IS NOT NULL " +
           "GROUP BY s.subreddit " +
           "ORDER BY avgEngagement DESC")
    List<Object[]> getSubredditStats();
    
    /**
     * Reddit-specific: Find posts with high upvote counts
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "s.platform = 'REDDIT' AND " +
           "s.upvotes > :minUpvotes " +
           "ORDER BY s.upvotes DESC")
    List<SocialPost> findHighUpvotePosts(@Param("minUpvotes") Long minUpvotes, Pageable pageable);
    
    // ===== YOUTUBE-SPECIFIC QUERIES =====
    
    /**
     * Find YouTube posts by video ID
     */
    @Query("SELECT s FROM SocialPost s WHERE s.platform = 'YOUTUBE' AND s.videoId = :videoId")
    Optional<SocialPost> findByVideoId(@Param("videoId") String videoId);
    
    /**
     * Find YouTube posts with high view counts
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "s.platform = 'YOUTUBE' AND " +
           "s.viewCount > :minViews " +
           "ORDER BY s.viewCount DESC")
    List<SocialPost> findHighViewPosts(@Param("minViews") Long minViews, Pageable pageable);
    
    // ===== ENGAGEMENT & TRENDING ANALYSIS =====
    
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
    
    /**
     * Find high engagement posts
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "s.engagementScore > :minScore " +
           "ORDER BY s.engagementScore DESC")
    List<SocialPost> findHighEngagementPosts(@Param("minScore") Double minScore, Pageable pageable);
    
    /**
     * Get top authors by engagement for a platform
     */
    @Query("SELECT s.author, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.platform = :platform " +
           "GROUP BY s.author " +
           "HAVING COUNT(s) > :minPosts " +
           "ORDER BY avgEngagement DESC")
    List<Object[]> getTopAuthorsByEngagement(@Param("platform") Platform platform, 
                                           @Param("minPosts") Long minPosts);
    
    // ===== TIME-SERIES & TREND ANALYTICS =====
    
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
    
    // ===== CROSS-PLATFORM ANALYTICS =====
    
    /**
     * Get platform comparison data (Reddit vs YouTube)
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
    
    // ===== SENTIMENT ANALYSIS SUPPORT (CORRECTED) =====
    
    /**
     * Find posts that don't have sentiment analysis yet
     * CORRECTED: Uses proper JOIN to check if sentiment data exists
     */
    @Query("SELECT sp FROM SocialPost sp " +
           "WHERE sp.id NOT IN (SELECT sd.socialPost.id FROM SentimentData sd) " +
           "AND (sp.content IS NOT NULL OR sp.title IS NOT NULL) " +
           "ORDER BY sp.ingestedAt DESC")
    List<SocialPost> findPostsWithoutSentiment(Pageable pageable);

    /**
     * Convenience method for finding posts without sentiment analysis
     */
    default List<SocialPost> findPostsWithoutSentiment(int limit) {
        return findPostsWithoutSentiment(PageRequest.of(0, limit));
    }
    
    /**
     * Alternative query for posts needing sentiment analysis (more explicit)
     */
    @Query("SELECT sp FROM SocialPost sp " +
           "LEFT JOIN SentimentData sd ON sd.socialPost.id = sp.id " +
           "WHERE sd.id IS NULL " +
           "AND (sp.content IS NOT NULL OR sp.title IS NOT NULL) " +
           "ORDER BY sp.createdAt DESC")
    List<SocialPost> findPostsNeedingSentimentAnalysis(Pageable pageable);
    
    // ===== BATCH OPERATIONS & UPDATES =====
    
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
           "WHERE s.processedAt IS NULL " +
           "AND s.id NOT IN (SELECT sd.socialPost.id FROM SentimentData sd)")
    int markUnprocessedForSentimentAnalysis();
}