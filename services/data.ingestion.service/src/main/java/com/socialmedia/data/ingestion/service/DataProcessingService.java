package com.socialmedia.data.ingestion.service;

import com.socialmedia.data.ingestion.dto.SocialPostDto;
import com.socialmedia.data.ingestion.model.Platform;
import com.socialmedia.data.ingestion.model.SocialPost;
import com.socialmedia.data.ingestion.repository.SocialPostRepository;
import com.socialmedia.data.ingestion.repository.SentimentDataRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simplified data processing service for MVP
 * Removed: complex analytics, advanced search, content feature extraction
 * Kept: core CRUD, basic validation, simple engagement calculation
 */
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
    
    // Simple constants
    private static final int MAX_CONTENT_LENGTH = 10000;
    
    /**
     * Save a single social post with basic validation
     */
    public SocialPostDto saveSocialPost(SocialPostDto postDto) {
        logger.info("Processing new social post from platform: {}", postDto.getPlatform());
        
        // Basic validation
        validateSocialPostDto(postDto);
        
        // Check for duplicates (simple check)
        if (socialPostRepository.existsByExternalIdAndPlatform(postDto.getExternalId(), postDto.getPlatform())) {
            logger.warn("Duplicate post detected for external ID: {}", postDto.getExternalId());
            throw new DuplicatePostException("Post with external ID " + postDto.getExternalId() + " already exists");
        }
        
        // Convert DTO to entity
        SocialPost entity = convertToEntity(postDto);
        
        // Calculate basic engagement score
        entity.setEngagementScore(calculateEngagementScore(entity));
        
        // Save to database
        SocialPost savedEntity = socialPostRepository.save(entity);
        logger.info("Successfully saved social post with ID: {}", savedEntity.getId());
        
        return convertToDto(savedEntity);
    }
    
    /**
     * Batch save multiple social posts
     */
    public List<SocialPostDto> saveSocialPosts(List<SocialPostDto> postDtos) {
        logger.info("Processing batch of {} social posts", postDtos.size());
        
        List<SocialPostDto> savedPosts = new ArrayList<>();
        
        for (SocialPostDto postDto : postDtos) {
            try {
                SocialPostDto saved = saveSocialPost(postDto);
                savedPosts.add(saved);
            } catch (Exception e) {
                logger.error("Error saving post {}: {}", postDto.getExternalId(), e.getMessage());
            }
        }
        
        logger.info("Successfully processed {}/{} posts in batch", savedPosts.size(), postDtos.size());
        return savedPosts;
    }
    
    /**
     * Get posts by platform (simple query)
     */
    public List<SocialPostDto> getPostsByPlatform(Platform platform, int limit) {
        logger.info("Fetching posts for platform: {}, limit: {}", platform, limit);
        
        List<SocialPost> posts = socialPostRepository.findByPlatformAndCreatedAtAfter(
            platform, LocalDateTime.now().minusDays(30));
        
        return posts.stream()
            .limit(limit)
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Update engagement metrics for existing post
     */
    public void updateEngagementMetrics(String externalId, Platform platform, SocialPostDto updates) {
        logger.info("Updating engagement metrics for post: {} on platform: {}", externalId, platform);
        
        Optional<SocialPost> existingPost = socialPostRepository.findByExternalIdAndPlatform(externalId, platform);
        
        if (existingPost.isEmpty()) {
            throw new PostNotFoundException("Post not found: " + externalId);
        }
        
        SocialPost post = existingPost.get();
        
        // Update engagement fields
        if (updates.getLikeCount() != null) post.setLikeCount(updates.getLikeCount());
        if (updates.getShareCount() != null) post.setShareCount(updates.getShareCount());
        if (updates.getCommentCount() != null) post.setCommentCount(updates.getCommentCount());
        if (updates.getUpvotes() != null) post.setUpvotes(updates.getUpvotes());
        if (updates.getViewCount() != null) post.setViewCount(updates.getViewCount());
        
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
        
        // Basic business validation
        if (postDto.getContent() != null && postDto.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new ValidationException("Content exceeds maximum length of " + MAX_CONTENT_LENGTH);
        }
        
        if (postDto.getCreatedAt() != null && postDto.getCreatedAt().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Created date cannot be in the future");
        }
    }
    
    private double calculateEngagementScore(SocialPost post) {
        // Simple platform-specific engagement calculation
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
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Simple formula: upvotes + (comments * 2)
        double score = upvotes + (comments * 2);
        return Math.max(0, Math.min(100, score / 10)); // Normalize to 0-100
    }
    
    private double calculateTwitterEngagementScore(SocialPost post) {
        long likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        long shares = post.getShareCount() != null ? post.getShareCount() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        // Simple formula: likes + (shares * 3) + (comments * 2)
        double score = likes + (shares * 3) + (comments * 2);
        return Math.max(0, Math.min(100, score / 20)); // Normalize to 0-100
    }
    
    private double calculateYouTubeEngagementScore(SocialPost post) {
        long likes = post.getLikeCount() != null ? post.getLikeCount() : 0;
        long views = post.getViewCount() != null ? post.getViewCount() : 0;
        long comments = post.getCommentCount() != null ? post.getCommentCount() : 0;
        
        if (views == 0) return 0.0;
        
        // Simple engagement rate
        double engagementRate = ((double) (likes + comments) / views) * 100;
        return Math.max(0, Math.min(100, engagementRate * 5));
    }
    
    private double calculateGenericEngagementScore(SocialPost post) {
        long totalEngagement = 0;
        
        if (post.getLikeCount() != null) totalEngagement += post.getLikeCount();
        if (post.getShareCount() != null) totalEngagement += post.getShareCount();
        if (post.getCommentCount() != null) totalEngagement += post.getCommentCount();
        if (post.getUpvotes() != null) totalEngagement += post.getUpvotes();
        
        // Simple logarithmic scale
        return totalEngagement > 0 ? Math.min(100, Math.log10(totalEngagement + 1) * 15) : 0.0;
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
        
        // Engagement metrics
        entity.setUpvotes(dto.getUpvotes());
        entity.setLikeCount(dto.getLikeCount());
        entity.setShareCount(dto.getShareCount());
        entity.setCommentCount(dto.getCommentCount());
        entity.setViewCount(dto.getViewCount());
        
        // Platform-specific fields
        entity.setSubreddit(dto.getSubreddit());
        entity.setVideoId(dto.getVideoId());
        
        return entity;
    }
    
    private SocialPostDto convertToDto(SocialPost entity) {
        return SocialPostDto.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .platform(entity.getPlatform())
                .title(entity.getTitle()) // Add this line
                .content(entity.getContent())
                .author(entity.getAuthor())
                .createdAt(entity.getCreatedAt())
                .upvotes(entity.getUpvotes())
                .likeCount(entity.getLikeCount())
                .shareCount(entity.getShareCount())
                .commentCount(entity.getCommentCount())
                .viewCount(entity.getViewCount())
                .engagementScore(entity.getEngagementScore())
                .subreddit(entity.getSubreddit())
                .videoId(entity.getVideoId())
                .build();
    }
}

// Simple exception classes
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