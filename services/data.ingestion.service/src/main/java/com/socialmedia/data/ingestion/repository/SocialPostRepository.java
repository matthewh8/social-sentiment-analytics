package com.socialmedia.data.ingestion.repository;

import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Simplified repository for SocialPost entities - MVP version
 * Removed: complex analytics, cross-platform comparisons, batch operations
 * Kept: essential CRUD, basic search, simple analytics
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
    
    // ===== PLATFORM & TIME-BASED QUERIES =====
    
    /**
     * Find posts by platform with pagination
     */
    Page<SocialPost> findByPlatform(Platform platform, Pageable pageable);
    
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
     * Find posts created between dates
     */
    List<SocialPost> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // ===== BASIC ANALYTICS =====
    
    /**
     * Count posts by platform since a specific date
     */
    @Query("SELECT COUNT(s) FROM SocialPost s WHERE s.platform = :platform AND s.createdAt > :since")
    Long countByPlatformSince(@Param("platform") Platform platform, @Param("since") LocalDateTime since);
    
    /**
     * Find high engagement posts above a threshold
     */
    @Query("SELECT s FROM SocialPost s WHERE s.engagementScore > :minScore ORDER BY s.engagementScore DESC")
    List<SocialPost> findHighEngagementPosts(@Param("minScore") Double minScore, Pageable pageable);
    
    /**
     * Find posts by author across platforms
     */
    List<SocialPost> findByAuthor(String author);
    
    // ===== CONTENT SEARCH =====
    
    /**
     * Find posts containing keywords in content or title
     */
    @Query("SELECT s FROM SocialPost s WHERE " +
           "(LOWER(s.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "s.platform = :platform " +
           "ORDER BY s.engagementScore DESC")
    List<SocialPost> findByContentContainingAndPlatform(@Param("keyword") String keyword, 
                                                        @Param("platform") Platform platform, 
                                                        Pageable pageable);
    
    // ===== REDDIT-SPECIFIC QUERIES =====
    
    /**
     * Find posts by subreddit
     */
    List<SocialPost> findBySubreddit(String subreddit, Pageable pageable);
    
    /**
     * Get top authors by engagement for a platform
     */
    @Query("SELECT s.author, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.platform = :platform " +
           "GROUP BY s.author " +
           "HAVING COUNT(s) > :minPosts " +
           "ORDER BY avgEngagement DESC")
    List<Object[]> getTopAuthorsByEngagement(@Param("platform") Platform platform, @Param("minPosts") Long minPosts);
    
    // ===== SIMPLE TRENDING ANALYSIS =====
    
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
    
    // ===== MINIMAL ANALYTICS FOR REPORTS =====
    
    /**
     * Get basic platform comparison data
     */
    @Query("SELECT s.platform, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.createdAt > :since " +
           "GROUP BY s.platform")
    List<Object[]> getPlatformComparisonData(@Param("since") LocalDateTime since);
    
    /**
     * Get basic volume statistics
     */
    @Query("SELECT " +
           "COUNT(s) as totalPosts, " +
           "COUNT(DISTINCT s.author) as uniqueAuthors, " +
           "AVG(s.engagementScore) as avgEngagement " +
           "FROM SocialPost s " +
           "WHERE s.createdAt > :since")
    Object[] getVolumeStatistics(@Param("since") LocalDateTime since);
    
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
     * Get engagement trends over time (simplified)
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
     * Find posts that need sentiment analysis
     */
    @Query("SELECT s FROM SocialPost s WHERE s.sentimentData IS NULL AND s.content IS NOT NULL ORDER BY s.createdAt DESC")
    List<SocialPost> findPostsNeedingSentimentAnalysis(Pageable pageable);
}