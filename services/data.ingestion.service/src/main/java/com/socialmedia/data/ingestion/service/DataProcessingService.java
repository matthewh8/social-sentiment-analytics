// service/DataProcessingService.java - Business logic layer with validation, duplicate detection, analytics
package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.dto.*;
import com.socialmedia.data.ingestion.model.*;
import com.socialmedia.data.ingestion.repository.SocialPostRepository;
import com.socialmedia.data.ingestion.repository.SentimentDataRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DataProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataProcessingService.class);
    
    @Autowired
    private SocialPostRepository socialPostRepository;
    
    @Autowired
    private SentimentDataRepository sentimentDataRepository;
    
    @Autowired
    private Validator validator;
    
    // Configuration constants
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final double DUPLICATE_SIMILARITY_THRESHOLD = 0.85;
    private static final int MAX_SEARCH_RESULTS = 1000;
    
    /**
     * Save a new social post with comprehensive validation and duplicate detection
     */
    public SocialPostDto saveSocialPost(SocialPostDto postDto) {
        logger.info("Processing new social post from platform: {}", postDto.getPlatform());
        
        // Validate input
        validateSocialPostDto(postDto);
        
        // Check for duplicates
        if (isDuplicate(postDto)) {
            logger.warn("Duplicate post detected for external ID: {}", postDto.getExternalId());
            throw new DuplicatePostException("Post with external ID " + postDto.getExternalId() + " already exists");
        }
        
        // Convert DTO to entity
        SocialPost entity = convertToEntity(postDto);
        
        // Calculate engagement score
        entity.setEngagementScore(calculateEngagementScore(entity));
        
        // Extract and process content features
        processContentFeatures(entity);
        
        // Save to database
        SocialPost savedEntity = socialPostRepository.save(entity);
        logger.info("Successfully saved social post with ID: {}", savedEntity.getId());
        
        return convertToDto(savedEntity);
    }
    
    /**
     * Batch save multiple social posts with optimized processing
     */
    public List<SocialPostDto> saveSocialPosts(List<SocialPostDto> postDtos) {
        logger.info("Processing batch of {} social posts", postDtos.size());
        
        List<SocialPostDto> savedPosts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (SocialPostDto postDto : postDtos) {
            try {
                SocialPostDto saved = saveSocialPost(postDto);
                savedPosts.add(saved);
            } catch (Exception e) {
                errors.add("Failed to save post " + postDto.getExternalId() + ": " + e.getMessage());
                logger.error("Error saving post {}: {}", postDto.getExternalId(), e.getMessage());
            }
        }
        
        if (!errors.isEmpty()) {
            logger.warn("Batch processing completed with {} errors: {}", errors.size(), errors);
        }
        
        logger.info("Successfully processed {}/{} posts in batch", savedPosts.size(), postDtos.size());
        return savedPosts;
    }
    
    /**
     * Search social posts with advanced criteria
     */
    public Page<SocialPostDto> searchPosts(PostSearchCriteria criteria) {
        logger.info("Searching posts with criteria: platforms={}, dateRange={} to {}", 
                   criteria.getPlatforms(), criteria.getStartDate(), criteria.getEndDate());
        
        // Validate search criteria
        if (!criteria.isValid()) {
            throw new IllegalArgumentException("Invalid search criteria provided");
        }
        
        // Create pageable object
        Sort sort = createSort(criteria.getSortBy(), criteria.getSortDirection());
        Pageable pageable = PageRequest.of(criteria.getPage(), 
                                         Math.min(criteria.getSize(), MAX_SEARCH_RESULTS), 
                                         sort);
        
        // Execute search based on criteria complexity
        Page<SocialPost> results = executeSearch(criteria, pageable);
        
        // Convert to DTOs
        Page<SocialPostDto> dtoResults = results.map(this::convertToDto);
        
        logger.info("Search completed: found {} results out of {} total", 
                   results.getNumberOfElements(), results.getTotalElements());
        
        return dtoResults;
    }
    
    /**
     * Generate comprehensive analytics report
     */
    public AnalyticsReport generateAnalyticsReport(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating analytics report for period: {} to {}", startDate, endDate);
        
        AnalyticsReport report = new AnalyticsReport(startDate, endDate);
        
        try {
            // Basic metrics
            populateBasicMetrics(report, startDate, endDate);
            
            // Platform breakdown
            populatePlatformMetrics(report, startDate, endDate);
            
            // Sentiment analysis
            populateSentimentMetrics(report, startDate, endDate);
            
            // Engagement statistics
            populateEngagementMetrics(report, startDate, endDate);
            
            // Top performers
            populateTopPerformers(report, startDate, endDate);
            
            // Time-based trends
            populateTrends(report, startDate, endDate);
            
            logger.info("Analytics report generated successfully with {} total posts analyzed", 
                       report.getTotalPosts());
            
        } catch (Exception e) {
            logger.error("Error generating analytics report: {}", e.getMessage(), e);
            throw new AnalyticsException("Failed to generate analytics report", e);
        }
        
        return report;
    }
    
    /**
     * Get engagement statistics for a specific platform
     */
    public EngagementStats getEngagementStats(Platform platform, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Calculating engagement stats for platform: {} from {} to {}", platform, startDate, endDate);
        
        List<SocialPost> posts = socialPostRepository.findByPlatformAndCreatedAtBetween(platform, startDate, endDate);
        
        if (posts.isEmpty()) {
            logger.warn("No posts found for platform {} in specified date range", platform);
            return new EngagementStats();
        }
        
        return calculateEngagementStats(posts);
    }
    
    /**
     * Detect and handle duplicate posts
     */
    public List<SocialPostDto> findDuplicates(Platform platform, int days) {
        logger.info("Finding duplicates for platform: {} within last {} days", platform, days);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<SocialPost> recentPosts = socialPostRepository.findByPlatformAndCreatedAtAfter(platform, cutoffDate);
        
        List<SocialPost> duplicates = new ArrayList<>();
        Map<String, SocialPost> contentHashes = new HashMap<>();
        
        for (SocialPost post : recentPosts) {
            String contentHash = generateContentHash(post.getContent());
            
            if (contentHashes.containsKey(contentHash)) {
                duplicates.add(post);
                logger.debug("Duplicate detected: {} matches {}", 
                           post.getExternalId(), contentHashes.get(contentHash).getExternalId());
            } else {
                contentHashes.put(contentHash, post);
            }
        }
        
        logger.info("Found {} duplicates out of {} posts analyzed", duplicates.size(), recentPosts.size());
        return duplicates.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * Update engagement metrics for existing posts
     */
    public void updateEngagementMetrics(String externalId, Platform platform, SocialPostDto updates) {
        logger.info("Updating engagement metrics for post: {} on platform: {}", externalId, platform);
        
        Optional<SocialPost> existingPost = socialPostRepository.findByExternalIdAndPlatform(externalId, platform);
        
        if (existingPost.isEmpty()) {
            logger.warn("Post not found for update: {} on platform: {}", externalId, platform);
            throw new PostNotFoundException("Post not found: " + externalId);
        }
        
        SocialPost post = existingPost.get();
        
        // Update engagement fields
        if (updates.getLikeCount() != null) post.setLikeCount(updates.getLikeCount());
        if (updates.getShareCount() != null) post.setShareCount(updates.getShareCount());
        if (updates.getCommentCount() != null) post.setCommentCount(updates.getCommentCount());
        if (updates.getUpvotes() != null) post.setUpvotes(updates.getUpvotes());
        if (updates.getDownvotes() != null) post.setDownvotes(updates.getDownvotes());
        if (updates.getViewCount() != null) post.setViewCount(updates.getViewCount());
        
        // Recalculate engagement score
        post.setEngagementScore(calculateEngagementScore(post));
        
        socialPostRepository.save(post);
        logger.info("Successfully updated engagement metrics for post: {}", externalId);
    }
    
    // Private helper methods
    
    private void validateSocialPostDto(SocialPostDto postDto) {
        Set<ConstraintViolation<SocialPostDto>> violations = validator.validate(postDto);
        
        if (!violations.isEmpty()) {
            String errorMessage = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new ValidationException("Validation failed: " + errorMessage);
        }
        
        // Additional business validation
        if (postDto.getContent() != null && postDto.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new ValidationException("Content exceeds maximum length of " + MAX_CONTENT_LENGTH);
        }
        
        if (postDto.getCreatedAt() != null && postDto.getCreatedAt().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Created date cannot be in the future");
        }
    }
    
    private boolean isDuplicate(SocialPostDto postDto) {
        // Check by external ID and platform
        Optional<SocialPost> existingById = socialPostRepository.findByExternalIdAndPlatform(
                postDto.getExternalId(), postDto.getPlatform());
        
        if (existingById.isPresent()) {
            return true;
        }
        
        // Check by content similarity for recent posts
        if (StringUtils.hasText(postDto.getContent())) {
            LocalDateTime recentCutoff = LocalDateTime.now().minusDays(7);
            List<SocialPost> recentPosts = socialPostRepository.findByPlatformAndCreatedAtAfter(
                    postDto.getPlatform(), recentCutoff);
            
            for (SocialPost recentPost : recentPosts) {
                double similarity = calculateContentSimilarity(postDto.getContent(), recentPost.getContent());
                if (similarity > DUPLICATE_SIMILARITY_THRESHOLD) {
                    logger.debug("High content similarity detected: {} vs {} ({})", 
                               postDto.getExternalId(), recentPost.getExternalId(), similarity);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private double calculateContentSimilarity(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return 0.0;
        }
        
        // Simple Jaccard similarity implementation
        Set<String> words1 = Arrays.stream(content1.toLowerCase().split("\\s+"))
                .collect(Collectors.toSet());
        Set<String> words2 = Arrays.stream(content2.toLowerCase().split("\\s+"))
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private String generateContentHash(String content) {
        if (content == null) return "";
        
        // Normalize content and create simple hash
        String normalized = content.toLowerCase()
                .replaceAll("[^\\w\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
        
        return String.valueOf(normalized.hashCode());
    }
    
    private double calculateEngagementScore(SocialPost post) {
        // Platform-specific engagement calculation
        switch (post.getPlatform()) {
            case REDDIT:
                return calculateRedditEngagementScore(post);
            case TWITTER:
                return calculateTwitterEngagementScore(post);
            case YOUTUBE:
                return calculateYouTubeEngagementScore(post);
            default:
                return calculateGenericEngagementScore(post);
        }
    }
    
    private double calculateRedditEngagementScore(SocialPost post) {
        long upvotes = post.getUpvotes() != null ? post.getUpvotes() : 0;
        long downvotes = post.getDownvotes() != null ? post.getDownvotes() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Reddit-specific formula: weighted score considering upvote ratio and comments
        long netVotes = upvotes - downvotes;
        double upvoteRatio = (upvotes + downvotes) > 0 ? (double) upvotes / (upvotes + downvotes) : 0.5;
        
        // Score components: net votes (60%), upvote ratio (25%), comments (15%)
        double score = (netVotes * 0.6) + (upvoteRatio * 100 * 0.25) + (comments * 0.15);
        
        return Math.max(0, Math.min(100, score));
    }
    
    private double calculateTwitterEngagementScore(SocialPost post) {
        long likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        long shares = post.getShareCount() != null ? post.getShareCount() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Twitter formula: weighted engagement with retweets having higher value
        double score = (likes * 0.4) + (shares * 0.4) + (comments * 0.2);
        
        return Math.max(0, Math.min(100, score / 10)); // Normalize to 0-100 scale
    }
    
    private double calculateYouTubeEngagementScore(SocialPost post) {
        long likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        long views = post.getViewCount() != null ? post.getViewCount() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        if (views == 0) return 0.0;
        
        // YouTube formula: engagement rate based on views
        double engagementRate = ((double) (likes + comments) / views) * 100;
        
        return Math.max(0, Math.min(100, engagementRate * 10)); // Scale appropriately
    }
    
    private double calculateGenericEngagementScore(SocialPost post) {
        long totalEngagement = 0;
        
        if (post.getLikeCount() != null) totalEngagement += post.getLikeCount();
        if (post.getShareCount() != null) totalEngagement += post.getShareCount();
        if (post.getCommentCount() != null) totalEngagement += post.getCommentCount();
        if (post.getUpvotes() != null) totalEngagement += post.getUpvotes();
        
        // Generic logarithmic scale
        return totalEngagement > 0 ? Math.min(100, Math.log10(totalEngagement + 1) * 20) : 0.0;
    }
    
    private void processContentFeatures(SocialPost post) {
        if (!StringUtils.hasText(post.getContent())) {
            return;
        }
        
        String content = post.getContent();
        
        // Extract hashtags
        Set<String> hashtags = new HashSet<>(extractHashtags(content));
        if (!hashtags.isEmpty()) {
            post.setHashtags(hashtags);
        }
        
        // Extract mentions
        Set<String> mentions = new HashSet<>(extractMentions(content));
        if (!mentions.isEmpty()) {
            post.setMentions(mentions);
        }
        
        // Extract topics (simple keyword-based approach)
        Set<String> topics = new HashSet<>(extractTopics(content));
        if (!topics.isEmpty()) {
            post.setTopicTags(topics);
        }
    }
    
    private List<String> extractHashtags(String content) {
        return Arrays.stream(content.split("\\s+"))
                .filter(word -> word.startsWith("#") && word.length() > 1)
                .map(word -> word.substring(1).toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }
    
    private List<String> extractMentions(String content) {
        return Arrays.stream(content.split("\\s+"))
                .filter(word -> word.startsWith("@") && word.length() > 1)
                .map(word -> word.substring(1).toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }
    
    private List<String> extractTopics(String content) {
        // Simple topic extraction based on common keywords
        Set<String> topicKeywords = Set.of(
                "technology", "tech", "ai", "artificial intelligence", "machine learning",
                "politics", "election", "government", "policy",
                "sports", "football", "basketball", "soccer", "tennis",
                "entertainment", "movie", "music", "celebrity",
                "business", "startup", "finance", "economy", "market",
                "health", "medical", "covid", "pandemic", "vaccine",
                "environment", "climate", "sustainability", "green"
        );
        
        String lowerContent = content.toLowerCase();
        return topicKeywords.stream()
                .filter(lowerContent::contains)
                .collect(Collectors.toList());
    }
    
    private SocialPost convertToEntity(SocialPostDto dto) {
        SocialPost entity = new SocialPost();
        
        entity.setExternalId(dto.getExternalId());
        entity.setPlatform(dto.getPlatform());
        entity.setContent(dto.getContent());
        entity.setAuthor(dto.getAuthor());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setIngestedAt(dto.getIngestedAt() != null ? dto.getIngestedAt() : LocalDateTime.now());
        
        // Engagement metrics
        entity.setUpvotes(dto.getUpvotes());
        entity.setDownvotes(dto.getDownvotes());
        entity.setLikeCount(dto.getLikeCount());
        entity.setShareCount(dto.getShareCount());
        entity.setCommentCount(dto.getCommentCount());
        
        // Platform-specific fields
        entity.setSubreddit(dto.getSubreddit());
        entity.setViewCount(dto.getViewCount());
        entity.setVideoId(dto.getVideoId());
        
        // Content features - convert Lists to Sets
        if (dto.getHashtags() != null) {
            entity.setHashtags(new HashSet<>(dto.getHashtags()));
        }
        if (dto.getMentions() != null) {
            entity.setMentions(new HashSet<>(dto.getMentions()));
        }
        if (dto.getTopics() != null) {
            entity.setTopicTags(new HashSet<>(dto.getTopics()));
        }
        
        return entity;
    }
    
    private SocialPostDto convertToDto(SocialPost entity) {
        return SocialPostDto.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .platform(entity.getPlatform())
                .content(entity.getContent())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .upvotes(entity.getUpvotes())
                .likeCount(entity.getLikeCount())
                .subreddit(entity.getSubreddit())
                .hashtags(entity.getHashtags() != null ? new ArrayList<>(entity.getHashtags()) : null)
                .build();
    }
    
    // Additional helper methods for analytics report generation would continue here...
    // (populateBasicMetrics, populatePlatformMetrics, etc.)
    
    private Sort createSort(String sortBy, PostSearchCriteria.SortDirection direction) {
        Sort.Direction sortDirection = direction == PostSearchCriteria.SortDirection.ASC ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, sortBy);
    }
    
    private Page<SocialPost> executeSearch(PostSearchCriteria criteria, Pageable pageable) {
        // Use your existing repository methods based on criteria
        
        // Content keyword search
        if (StringUtils.hasText(criteria.getContentKeyword()) && criteria.getPlatforms() != null) {
            Platform platform = criteria.getPlatforms().get(0); // Use first platform for keyword search
            List<SocialPost> results = socialPostRepository.findByContentContainingAndPlatform(
                criteria.getContentKeyword(), platform, pageable);
            return new PageImpl<>(results, pageable, results.size());
        }
        
        // Hashtag search
        if (criteria.getHashtags() != null && !criteria.getHashtags().isEmpty()) {
            String hashtag = criteria.getHashtags().get(0); // Use first hashtag
            List<SocialPost> results = socialPostRepository.findByHashtag(hashtag);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), results.size());
            return new PageImpl<>(results.subList(start, end), pageable, results.size());
        }
        
        // Author search
        if (StringUtils.hasText(criteria.getAuthor())) {
            List<SocialPost> results = socialPostRepository.findByAuthor(criteria.getAuthor());
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), results.size());
            return new PageImpl<>(results.subList(start, end), pageable, results.size());
        }
        
        // Subreddit search (Reddit-specific)
        if (StringUtils.hasText(criteria.getSubreddit())) {
            List<SocialPost> results = socialPostRepository.findBySubreddit(criteria.getSubreddit(), pageable);
            return new PageImpl<>(results, pageable, results.size());
        }
        
        // High engagement search
        if (criteria.getMinEngagementScore() != null) {
            List<SocialPost> results = socialPostRepository.findHighEngagementPosts(
                criteria.getMinEngagementScore(), pageable);
            return new PageImpl<>(results, pageable, results.size());
        }
        
        // Trending posts search
        if (criteria.getStartDate() != null && criteria.getMinEngagementScore() != null) {
            List<SocialPost> results = socialPostRepository.findTrendingPosts(
                criteria.getStartDate(), criteria.getMinEngagementScore(), pageable);
            return new PageImpl<>(results, pageable, results.size());
        }
        
        // Platform and date range search
        if (criteria.getPlatforms() != null && !criteria.getPlatforms().isEmpty()) {
            if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                // This method needs to be added to repository
                List<SocialPost> results = new ArrayList<>();
                for (Platform platform : criteria.getPlatforms()) {
                    List<SocialPost> platformPosts = socialPostRepository.findByPlatformAndCreatedAtBetween(
                        platform, criteria.getStartDate(), criteria.getEndDate());
                    results.addAll(platformPosts);
                }
                // Sort and paginate manually
                results.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), results.size());
                return new PageImpl<>(results.subList(start, end), pageable, results.size());
            } else {
                return socialPostRepository.findByPlatform(criteria.getPlatforms().get(0), pageable);
            }
        } 
        
        // Date range only
        else if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
            List<SocialPost> results = socialPostRepository.findByCreatedAtBetween(
                criteria.getStartDate(), criteria.getEndDate());
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), results.size());
            return new PageImpl<>(results.subList(start, end), pageable, results.size());
        } 
        
        // Default: return all with pagination
        else {
            return socialPostRepository.findAll(pageable);
        }
    }
    
    private EngagementStats calculateEngagementStats(List<SocialPost> posts) {
        EngagementStats stats = new EngagementStats();
        
        if (posts.isEmpty()) {
            return stats;
        }
        
        // Calculate basic statistics
        double[] engagementScores = posts.stream()
                .mapToDouble(post -> post.getEngagementScore() != null ? post.getEngagementScore() : 0.0)
                .toArray();
        
        Arrays.sort(engagementScores);
        
        stats.setAverageEngagementScore(Arrays.stream(engagementScores).average().orElse(0.0));
        stats.setMedianEngagementScore(engagementScores[engagementScores.length / 2]);
        stats.setMaxEngagementScore(engagementScores[engagementScores.length - 1]);
        stats.setMinEngagementScore(engagementScores[0]);
        
        // Calculate totals
        stats.setTotalLikes(posts.stream().mapToLong(p -> p.getLikeCount() != null ? p.getLikeCount() : 0).sum());
        stats.setTotalComments(posts.stream().mapToLong(p -> p.getCommentCount() != null ? p.getCommentCount() : 0).sum());
        stats.setTotalShares(posts.stream().mapToLong(p -> p.getShareCount() != null ? p.getShareCount() : 0).sum());
        
        return stats;
    }
    
    // Placeholder methods for analytics report population
    private void populateBasicMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Use your existing getVolumeStatistics method
        Object[] stats = socialPostRepository.getVolumeStatistics(start);
        if (stats != null && stats.length >= 4) {
            report.setTotalPosts((Long) stats[0]);
            report.setTotalAuthors((Long) stats[1]);
            report.setAverageEngagementScore((Double) stats[3]);
        } else {
            // Fallback to simple count
            List<SocialPost> posts = socialPostRepository.findByCreatedAtBetween(start, end);
            report.setTotalPosts((long) posts.size());
            report.setTotalAuthors((long) posts.stream().map(SocialPost::getAuthor).distinct().count());
            report.setAverageEngagementScore(posts.stream()
                .mapToDouble(p -> p.getEngagementScore() != null ? p.getEngagementScore() : 0.0)
                .average().orElse(0.0));
        }
    }
    
    private void populatePlatformMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Use your existing getPlatformComparisonData method
        List<Object[]> platformData = socialPostRepository.getPlatformComparisonData(start);
        
        Map<Platform, Long> postsByPlatform = new HashMap<>();
        Map<Platform, Double> avgEngagementByPlatform = new HashMap<>();
        
        for (Object[] row : platformData) {
            Platform platform = (Platform) row[0];
            Long postCount = (Long) row[1];
            Double avgEngagement = (Double) row[2];
            
            postsByPlatform.put(platform, postCount);
            avgEngagementByPlatform.put(platform, avgEngagement);
        }
        
        report.setPostsByPlatform(postsByPlatform);
        report.setAvgEngagementByPlatform(avgEngagementByPlatform);
    }
    
    private void populateSentimentMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Use sentiment repository to get sentiment distribution
        Map<SentimentLabel, Long> sentimentDistribution = new HashMap<>();
        Map<Platform, Double> sentimentByPlatform = new HashMap<>();
        
        // This would use your SentimentDataRepository when implemented
        // For now, set defaults
        sentimentDistribution.put(SentimentLabel.POSITIVE, 0L);
        sentimentDistribution.put(SentimentLabel.NEGATIVE, 0L);
        sentimentDistribution.put(SentimentLabel.NEUTRAL, 0L);
        
        report.setSentimentDistribution(sentimentDistribution);
        report.setSentimentByPlatform(sentimentByPlatform);
        report.setOverallSentimentScore(0.5); // Neutral baseline
    }
    
    private void populateEngagementMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        List<SocialPost> posts = socialPostRepository.findByCreatedAtBetween(start, end);
        EngagementStats stats = calculateEngagementStats(posts);
        report.setEngagementStats(stats);
    }
    
    private void populateTopPerformers(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Top authors using your existing getTopAuthorsByEngagement method
        List<AnalyticsReport.TopAuthor> topAuthors = new ArrayList<>();
        for (Platform platform : Platform.values()) {
            List<Object[]> authorData = socialPostRepository.getTopAuthorsByEngagement(platform, 5L);
            for (Object[] row : authorData) {
                if (topAuthors.size() < 10) { // Limit to top 10 overall
                    String author = (String) row[0];
                    Long postCount = (Long) row[1];
                    Double avgEngagement = (Double) row[2];
                    topAuthors.add(new AnalyticsReport.TopAuthor(author, postCount, avgEngagement, platform));
                }
            }
        }
        report.setTopAuthors(topAuthors);
        
        // Top posts using your existing findHighEngagementPosts method
        List<SocialPost> topPostEntities = socialPostRepository.findHighEngagementPosts(50.0, 
            PageRequest.of(0, 10));
        List<AnalyticsReport.TopContent> topPosts = topPostEntities.stream()
            .map(post -> new AnalyticsReport.TopContent(
                post.getId(),
                post.getContent(),
                post.getAuthor(),
                post.getPlatform(),
                post.getEngagementScore(),
                SentimentLabel.NEUTRAL // Default until sentiment analysis is implemented
            ))
            .collect(Collectors.toList());
        report.setTopPosts(topPosts);
        
        // Reddit-specific top subreddits using your existing getSubredditStats method
        List<Object[]> subredditData = socialPostRepository.getSubredditStats();
        List<AnalyticsReport.SubredditStats> topSubreddits = subredditData.stream()
            .limit(10)
            .map(row -> new AnalyticsReport.SubredditStats(
                (String) row[0],
                (Long) row[1],
                (Double) row[2]
            ))
            .collect(Collectors.toList());
        report.setTopSubreddits(topSubreddits);
    }
    
    private void populateTrends(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Use your existing getEngagementTrends method
        List<AnalyticsReport.TrendPoint> volumeTrend = new ArrayList<>();
        List<AnalyticsReport.TrendPoint> sentimentTrend = new ArrayList<>();
        
        for (Platform platform : Platform.values()) {
            List<Object[]> trendData = socialPostRepository.getEngagementTrends(platform, start, end);
            for (Object[] row : trendData) {
                LocalDateTime date = (LocalDateTime) row[0];
                Double avgEngagement = (Double) row[1];
                Long postCount = (Long) row[2];
                
                volumeTrend.add(new AnalyticsReport.TrendPoint(date, postCount.doubleValue(), postCount));
                sentimentTrend.add(new AnalyticsReport.TrendPoint(date, avgEngagement, postCount));
            }
        }
        
        report.setVolumeTrend(volumeTrend);
        report.setSentimentTrend(sentimentTrend);
    }
}

// Custom exception classes
class DuplicatePostException extends RuntimeException {
    public DuplicatePostException(String message) {
        super(message);
    }
}

class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String message) {
        super(message);
    }
}

class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

class AnalyticsException extends RuntimeException {
    public AnalyticsException(String message, Throwable cause) {
        super(message, cause);
    }
}