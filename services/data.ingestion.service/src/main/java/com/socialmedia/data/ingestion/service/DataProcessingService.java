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
    private StanfordSentimentService sentimentService;
    
    @Autowired
    private Validator validator;
    
    // Configuration constants
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final double DUPLICATE_SIMILARITY_THRESHOLD = 0.85;
    private static final int MAX_SEARCH_RESULTS = 1000;
    
    /**
     * Save a social post WITH automatic sentiment analysis
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
        
        // Save to database FIRST
        SocialPost savedEntity = socialPostRepository.save(entity);
        logger.info("Successfully saved social post with ID: {}", savedEntity.getId());
        
        // THEN analyze sentiment asynchronously
        try {
            sentimentService.analyzeSentiment(savedEntity);
            logger.debug("Triggered sentiment analysis for post: {}", savedEntity.getId());
        } catch (Exception e) {
            logger.warn("Failed to trigger sentiment analysis for post {}: {}", savedEntity.getId(), e.getMessage());
            // Don't fail the entire operation if sentiment analysis fails
        }
        
        return convertToDto(savedEntity);
    }   
    /**
     * Batch save with sentiment analysis
     */
    public List<SocialPostDto> saveSocialPosts(List<SocialPostDto> postDtos) {
        logger.info("Processing batch of {} social posts", postDtos.size());
        
        List<SocialPostDto> savedPosts = new ArrayList<>();
        List<SocialPost> postsForSentiment = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (SocialPostDto postDto : postDtos) {
            try {
                // Save without sentiment analysis first
                SocialPost entity = convertToEntity(postDto);
                entity.setEngagementScore(calculateEngagementScore(entity));
                processContentFeatures(entity);
                
                if (!isDuplicate(postDto)) {
                    SocialPost saved = socialPostRepository.save(entity);
                    savedPosts.add(convertToDto(saved));
                    postsForSentiment.add(saved);
                }
            } catch (Exception e) {
                errors.add("Failed to save post " + postDto.getExternalId() + ": " + e.getMessage());
                logger.error("Error saving post {}: {}", postDto.getExternalId(), e.getMessage());
            }
        }
        
        // Trigger batch sentiment analysis if we have posts
        if (!postsForSentiment.isEmpty()) {
            try {
                sentimentService.analyzeSentimentBatch(postsForSentiment);
                logger.info("Triggered batch sentiment analysis for {} posts", postsForSentiment.size());
            } catch (Exception e) {
                logger.warn("Failed to trigger batch sentiment analysis: {}", e.getMessage());
            }
        }
        
        if (!errors.isEmpty()) {
            logger.warn("Batch processing completed with {} errors", errors.size());
        }
        
        logger.info("Successfully processed {}/{} posts in batch", savedPosts.size(), postDtos.size());
        return savedPosts;
    }

    /**
     * Process unanalyzed posts for sentiment
     */
    public Map<String, Object> processPendingSentimentAnalysis(int batchSize) {
        logger.info("Processing pending sentiment analysis for up to {} posts", batchSize);
        
        try {
            List<SocialPost> unanalyzedPosts = socialPostRepository.findPostsWithoutSentiment(batchSize);
            
            if (unanalyzedPosts.isEmpty()) {
                return Map.of(
                    "status", "info",
                    "message", "No posts found requiring sentiment analysis",
                    "processed", 0
                );
            }
            
            // Trigger batch sentiment analysis
            sentimentService.analyzeSentimentBatch(unanalyzedPosts);
            
            return Map.of(
                "status", "success", 
                "message", "Batch sentiment analysis started",
                "processed", unanalyzedPosts.size(),
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to process pending sentiment analysis: {}", e.getMessage());
            
            return Map.of(
                "status", "error",
                "message", "Failed to process sentiment analysis: " + e.getMessage(),
                "processed", 0
            );
        }
    }

    /**
     * Get sentiment analysis progress
     */
    public Map<String, Object> getSentimentAnalysisProgress() {
        try {
            long totalPosts = socialPostRepository.count();
            long analyzedPosts = sentimentDataRepository.count();
            long pendingPosts = totalPosts - analyzedPosts;
            
            double completionRate = totalPosts > 0 ? ((double) analyzedPosts / totalPosts) * 100 : 0.0;
            
            return Map.of(
                "totalPosts", totalPosts,
                "analyzedPosts", analyzedPosts,
                "pendingPosts", pendingPosts,
                "completionPercentage", Math.round(completionRate * 100.0) / 100.0,
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error getting sentiment analysis progress: {}", e.getMessage());
            
            return Map.of(
                "error", "Unable to retrieve progress information",
                "timestamp", LocalDateTime.now()
            );
        }
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
        
        // Update engagement fields based on platform
        if (platform == Platform.REDDIT) {
            if (updates.getUpvotes() != null) post.setUpvotes(updates.getUpvotes());
        }
        
        if (platform == Platform.YOUTUBE) {
            if (updates.getLikeCount() != null) post.setLikeCount(updates.getLikeCount());
            if (updates.getViewCount() != null) post.setViewCount(updates.getViewCount());
        }
        
        // Common fields for both platforms
        if (updates.getShareCount() != null) post.setShareCount(updates.getShareCount());
        if (updates.getCommentCount() != null) post.setCommentCount(updates.getCommentCount());
        
        // Recalculate engagement score
        post.setEngagementScore(calculateEngagementScore(post));
        
        socialPostRepository.save(post);
        logger.info("Successfully updated engagement metrics for post: {}", externalId);
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
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
        
        // Platform-specific validation
        if (postDto.getPlatform() == Platform.REDDIT && !StringUtils.hasText(postDto.getSubreddit())) {
            throw new ValidationException("Subreddit is required for Reddit posts");
        }
        
        if (postDto.getPlatform() == Platform.YOUTUBE && !StringUtils.hasText(postDto.getVideoId())) {
            throw new ValidationException("Video ID is required for YouTube posts");
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
            case YOUTUBE:
                return calculateYouTubeEngagementScore(post);
            default:
                return calculateGenericEngagementScore(post);
        }
    }
    
    private double calculateRedditEngagementScore(SocialPost post) {
        long upvotes = post.getUpvotes() != null ? post.getUpvotes() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Reddit formula: upvotes + (comments * 2.5) - comments are valuable
        double score = upvotes + (comments * 2.5);
        
        // Logarithmic scaling for very high scores to normalize
        if (score > 1000) {
            score = 1000 + Math.log10(score - 999) * 100;
        }
        
        return Math.max(0, Math.min(10000, score));
    }
    
    private double calculateYouTubeEngagementScore(SocialPost post) {
        long likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        long views = post.getViewCount() != null ? post.getViewCount() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        if (views == 0) {
            // If no views, base score on likes and comments only
            return Math.max(0, Math.min(10000, (likes * 1.5) + (comments * 3.0)));
        }
        
        // YouTube formula: engagement rate based on views
        double engagementRate = ((double) (likes + comments)) / views;
        double baseEngagement = (likes * 1.5) + (comments * 3.0);
        
        // Scale engagement rate and combine with base
        double score = (engagementRate * views * 0.1) + baseEngagement;
        
        return Math.max(0, Math.min(10000, score));
    }
    
    private double calculateGenericEngagementScore(SocialPost post) {
        long totalEngagement = 0;
        
        if (post.getLikeCount() != null) totalEngagement += post.getLikeCount();
        if (post.getShareCount() != null) totalEngagement += post.getShareCount();
        if (post.getCommentCount() != null) totalEngagement += post.getCommentCount();
        if (post.getUpvotes() != null) totalEngagement += post.getUpvotes();
        
        // Generic logarithmic scale
        return totalEngagement > 0 ? Math.min(10000, Math.log10(totalEngagement + 1) * 200) : 0.0;
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
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setAuthor(dto.getAuthor());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setIngestedAt(dto.getIngestedAt() != null ? dto.getIngestedAt() : LocalDateTime.now());
        
        // Platform-specific engagement metrics
        if (dto.getPlatform() == Platform.REDDIT) {
            entity.setUpvotes(dto.getUpvotes());
            entity.setSubreddit(dto.getSubreddit());
        }
        
        if (dto.getPlatform() == Platform.YOUTUBE) {
            entity.setLikeCount(dto.getLikeCount());
            entity.setViewCount(dto.getViewCount());
            entity.setVideoId(dto.getVideoId());
        }
        
        // Common fields
        entity.setShareCount(dto.getShareCount());
        entity.setCommentCount(dto.getCommentCount());
        entity.setUrl(dto.getUrl());
        
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
                .title(entity.getTitle())
                .content(entity.getContent())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .upvotes(entity.getUpvotes())
                .likeCount(entity.getLikeCount())
                .commentCount(entity.getCommentCount())
                .shareCount(entity.getShareCount())
                .viewCount(entity.getViewCount())
                .subreddit(entity.getSubreddit())
                .videoId(entity.getVideoId())
                .url(entity.getUrl())
                .engagementScore(entity.getEngagementScore())
                .hashtags(entity.getHashtags() != null ? new ArrayList<>(entity.getHashtags()) : null)
                .build();
    }
    
    private Sort createSort(String sortBy, PostSearchCriteria.SortDirection direction) {
        Sort.Direction sortDirection = direction == PostSearchCriteria.SortDirection.ASC ? 
                Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, sortBy);
    }
    
    private Page<SocialPost> executeSearch(PostSearchCriteria criteria, Pageable pageable) {
        // Content keyword search
        if (StringUtils.hasText(criteria.getContentKeyword()) && criteria.getPlatforms() != null) {
            Platform platform = criteria.getPlatforms().get(0);
            List<SocialPost> results = socialPostRepository.findByContentContainingAndPlatform(
                criteria.getContentKeyword(), platform, pageable);
            return new PageImpl<>(results, pageable, results.size());
        }
        
        // Hashtag search
        if (criteria.getHashtags() != null && !criteria.getHashtags().isEmpty()) {
            String hashtag = criteria.getHashtags().get(0);
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
        
        // Platform and date range search
        if (criteria.getPlatforms() != null && !criteria.getPlatforms().isEmpty()) {
            if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
                List<SocialPost> results = new ArrayList<>();
                for (Platform platform : criteria.getPlatforms()) {
                    List<SocialPost> platformPosts = socialPostRepository.findByPlatformAndCreatedAtBetween(
                        platform, criteria.getStartDate(), criteria.getEndDate());
                    results.addAll(platformPosts);
                }
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
            return socialPostRepository.findByCreatedAtBetween(
                criteria.getStartDate(), criteria.getEndDate(), pageable);
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
        
        // Calculate platform-specific totals
        stats.setTotalLikes(posts.stream().mapToLong(p -> p.getLikeCount() != null ? p.getLikeCount() : 0).sum());
        stats.setTotalUpvotes(posts.stream().mapToLong(p -> p.getUpvotes() != null ? p.getUpvotes() : 0).sum());
        stats.setTotalComments(posts.stream().mapToLong(p -> p.getCommentCount() != null ? p.getCommentCount() : 0).sum());
        stats.setTotalShares(posts.stream().mapToLong(p -> p.getShareCount() != null ? p.getShareCount() : 0).sum());
        stats.setTotalViews(posts.stream().mapToLong(p -> p.getViewCount() != null ? p.getViewCount() : 0).sum());
        
        return stats;
    }
    
    // ===== ANALYTICS REPORT METHODS =====
    
    private void populateBasicMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        Object[] stats = socialPostRepository.getVolumeStatistics(start);
        if (stats != null && stats.length >= 4) {
            report.setTotalPosts((Long) stats[0]);
            report.setTotalAuthors((Long) stats[1]);
            report.setAverageEngagementScore((Double) stats[3]);
        } else {
            // Use count method instead of loading all posts
            Long totalPosts = socialPostRepository.countByCreatedAtBetween(start, end);
            report.setTotalPosts(totalPosts);
            
            // Get a sample for calculations if needed
            Pageable samplePageable = PageRequest.of(0, 1000);
            Page<SocialPost> samplePosts = socialPostRepository.findByCreatedAtBetween(start, end, samplePageable);
            
            report.setTotalAuthors((long) samplePosts.getContent().stream()
                .map(SocialPost::getAuthor).distinct().count());
            report.setAverageEngagementScore(samplePosts.getContent().stream()
                .mapToDouble(p -> p.getEngagementScore() != null ? p.getEngagementScore() : 0.0)
                .average().orElse(0.0));
        }
    }
    
    private void populatePlatformMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
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
        Map<SentimentLabel, Long> sentimentDistribution = new HashMap<>();
        Map<Platform, Double> sentimentByPlatform = new HashMap<>();
        
        try {
            // Get cross-platform sentiment comparison
            List<Object[]> crossPlatformData = sentimentDataRepository.getCrossPlatformSentimentComparison(start);
            
            double totalSentiment = 0.0;
            long totalCount = 0;
            
            for (Object[] row : crossPlatformData) {
                Platform platform = (Platform) row[0];
                Double avgSentiment = (Double) row[1];
                Long count = (Long) row[2];
                Long positiveCount = (Long) row[3];
                Long negativeCount = (Long) row[4];
                Long neutralCount = count - positiveCount - negativeCount;
                
                sentimentByPlatform.put(platform, avgSentiment);
                
                // Aggregate sentiment distribution
                sentimentDistribution.merge(SentimentLabel.POSITIVE, positiveCount, Long::sum);
                sentimentDistribution.merge(SentimentLabel.NEGATIVE, negativeCount, Long::sum);
                sentimentDistribution.merge(SentimentLabel.NEUTRAL, neutralCount, Long::sum);
                
                totalSentiment += avgSentiment * count;
                totalCount += count;
            }
            
            // Calculate overall sentiment score
            double overallSentiment = totalCount > 0 ? totalSentiment / totalCount : 0.5;
            report.setOverallSentimentScore(overallSentiment);
            
        } catch (Exception e) {
            logger.debug("Sentiment data not available, using defaults: {}", e.getMessage());
            
            // Provide default values
            sentimentDistribution.put(SentimentLabel.POSITIVE, 0L);
            sentimentDistribution.put(SentimentLabel.NEGATIVE, 0L);
            sentimentDistribution.put(SentimentLabel.NEUTRAL, 0L);
            report.setOverallSentimentScore(0.5); // Neutral baseline
        }
        
        report.setSentimentDistribution(sentimentDistribution);
        report.setSentimentByPlatform(sentimentByPlatform);
    }
    
    private void populateEngagementMetrics(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Use pageable to get a reasonable sample for engagement calculations
        Pageable engagementPageable = PageRequest.of(0, 5000); // Get up to 5000 posts for analysis
        Page<SocialPost> postsPage = socialPostRepository.findByCreatedAtBetween(start, end, engagementPageable);
        
        EngagementStats engagementStats = calculateEngagementStats(postsPage.getContent());
        report.setEngagementStats(engagementStats);
    }
    
    private void populateTopPerformers(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Top Authors
        List<AnalyticsReport.TopAuthor> topAuthors = new ArrayList<>();
        
        // Get top authors for Reddit
        List<Object[]> redditAuthors = socialPostRepository.getTopAuthorsByEngagement(Platform.REDDIT, 2L);
        for (Object[] row : redditAuthors.subList(0, Math.min(5, redditAuthors.size()))) {
            String author = (String) row[0];
            Long postCount = (Long) row[1];
            Double avgEngagement = (Double) row[2];
            topAuthors.add(new AnalyticsReport.TopAuthor(author, postCount, avgEngagement, Platform.REDDIT));
        }
        
        // Get top authors for YouTube
        List<Object[]> youtubeAuthors = socialPostRepository.getTopAuthorsByEngagement(Platform.YOUTUBE, 2L);
        for (Object[] row : youtubeAuthors.subList(0, Math.min(5, youtubeAuthors.size()))) {
            String author = (String) row[0];
            Long postCount = (Long) row[1];
            Double avgEngagement = (Double) row[2];
            topAuthors.add(new AnalyticsReport.TopAuthor(author, postCount, avgEngagement, Platform.YOUTUBE));
        }
        
        // Sort by engagement and take top 10
        topAuthors.sort((a, b) -> Double.compare(b.getAvgEngagementScore(), a.getAvgEngagementScore()));
        report.setTopAuthors(topAuthors.subList(0, Math.min(10, topAuthors.size())));
        
        // Top Posts
        Pageable topPostsPageable = PageRequest.of(0, 10);
        List<SocialPost> highEngagementPosts = socialPostRepository.findHighEngagementPosts(100.0, topPostsPageable);
        
        List<AnalyticsReport.TopPost> topPosts = highEngagementPosts.stream()
            .map(post -> new AnalyticsReport.TopPost(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getPlatform(),
                post.getEngagementScore()
            ))
            .collect(Collectors.toList());
        
        report.setTopPosts(topPosts);
        
        // Top Subreddits (Reddit specific)
        List<Object[]> subredditStats = socialPostRepository.getSubredditStats();
        List<AnalyticsReport.SubredditStats> topSubreddits = subredditStats.stream()
            .limit(10)
            .map(row -> new AnalyticsReport.SubredditStats(
                (String) row[0],     // subreddit name
                (Long) row[1],       // post count
                (Double) row[2]      // avg engagement
            ))
            .collect(Collectors.toList());
        
        report.setTopSubreddits(topSubreddits);
    }
    
    private void populateTrends(AnalyticsReport report, LocalDateTime start, LocalDateTime end) {
        // Sentiment trends over time
        List<AnalyticsReport.TrendPoint> sentimentTrend = new ArrayList<>();
        
        // Generate daily sentiment trend (mock data for now)
        LocalDateTime current = start;
        while (current.isBefore(end)) {
            LocalDateTime nextDay = current.plusDays(1);
            
            // Use count method instead of loading all posts
            Long dayPostCount = socialPostRepository.countByCreatedAtBetween(current, nextDay);
            
            // Calculate average sentiment for the day (would use actual sentiment data)
            double avgSentiment = 0.5; // Neutral default
            
            sentimentTrend.add(new AnalyticsReport.TrendPoint(current, avgSentiment, dayPostCount));
            current = nextDay;
        }
        
        report.setSentimentTrend(sentimentTrend);
        
        // Volume trends over time
        List<Object[]> dailyVolume = socialPostRepository.getDailyPostCounts(start, end);
        List<AnalyticsReport.TrendPoint> volumeTrend = dailyVolume.stream()
            .map(row -> {
                LocalDateTime date = (LocalDateTime) row[0];
                Long count = (Long) row[1];
                return new AnalyticsReport.TrendPoint(date, count.doubleValue(), count);
            })
            .collect(Collectors.toList());
        
        report.setVolumeTrend(volumeTrend);
    }
    
    // ===== CUSTOM EXCEPTION CLASSES =====
    
    public static class DuplicatePostException extends RuntimeException {
        public DuplicatePostException(String message) {
            super(message);
        }
        
        public DuplicatePostException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class PostNotFoundException extends RuntimeException {
        public PostNotFoundException(String message) {
            super(message);
        }
        
        public PostNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
        
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class AnalyticsException extends RuntimeException {
        public AnalyticsException(String message) {
            super(message);
        }
        
        public AnalyticsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
