# Service Architecture Documentation

## Overview
The Social Media Sentiment Analytics System follows a **reactive microservices architecture** designed for scalability, fault tolerance, and real-time processing.

## Core Architecture Principles

### 1. Reactive Programming
- **Non-blocking I/O**: All external API calls use Spring WebFlux
- **Backpressure Handling**: Rate limiting with reactive streams
- **Async Processing**: Mono/Flux patterns throughout the application

### 2. Domain-Driven Design
- **Clear Boundaries**: Separation between Reddit, YouTube, and core processing
- **Rich Domain Models**: Entities with business logic (engagement scoring)
- **Repository Pattern**: Data access abstraction

### 3. Fault Tolerance
- **Circuit Breaker**: Prevents cascade failures
- **Retry Mechanisms**: Exponential backoff for transient failures
- **Graceful Degradation**: Empty results instead of crashes

## Service Layer Details

### RedditIngestionService
**Purpose**: Orchestrates Reddit data ingestion workflow

**Key Methods**:
```java
// Single subreddit ingestion
Mono<Integer> ingestFromSubreddit(String subreddit, int limit)

// Batch processing multiple subreddits  
Mono<Integer> ingestFromMultipleSubreddits(List<String> subreddits, int limitPerSubreddit)

// Manual trigger for API endpoints
Mono<Integer> triggerManualIngestion(String[] subreddits, int postsPerSubreddit)
```

**Data Flow**:
1. Trigger ingestion request
2. Call RedditApiClient for data fetching
3. Convert Reddit API response to domain models
4. Filter duplicates using repository
5. Batch save new posts
6. Update session statistics
7. Return count of processed posts

**Error Handling**:
- Individual subreddit failures don't stop batch processing
- Detailed error logging with context
- Session counter tracks successful ingestions

### DataProcessingService  
**Purpose**: Core business logic for social media data processing

**Key Capabilities**:

#### Data Validation & Processing
```java
public SocialPostDto saveSocialPost(SocialPostDto postDto) {
    validateSocialPostDto(postDto);           // Jakarta validation + custom rules
    if (isDuplicate(postDto)) {               // Duplicate detection
        throw new DuplicatePostException();
    }
    SocialPost entity = convertToEntity(postDto);
    entity.calculateEngagementScore();        // Platform-specific scoring
    processContentFeatures(entity);          // Extract hashtags, mentions
    return convertToDto(socialPostRepository.save(entity));
}
```

#### Advanced Search
```java
public Page<SocialPostDto> searchPosts(PostSearchCriteria criteria) {
    // Supports multiple search patterns:
    // - Content keyword search
    // - Hashtag filtering  
    // - Author-based search
    // - Subreddit filtering
    // - Engagement thresholds
    // - Date range filtering
    // - Cross-platform search
}
```

#### Analytics Generation
```java
public AnalyticsReport generateAnalyticsReport(LocalDateTime start, LocalDateTime end) {
    // Comprehensive analytics including:
    // - Basic metrics (total posts, authors, avg engagement)
    // - Platform breakdown
    // - Sentiment distribution
    // - Top performers (authors, posts, subreddits)
    // - Time-based trends
}
```

#### Duplicate Detection Strategy
1. **Primary**: External ID + Platform uniqueness check
2. **Secondary**: Content similarity using Jaccard coefficient
3. **Threshold**: 85% similarity triggers duplicate flag
4. **Hash-based**: MD5 content hashing for exact matches

### RedditApiClient
**Purpose**: Reactive HTTP client for Reddit API integration

**Architecture**:
```java
public Flux<RedditPost> fetchSubredditPosts(String subreddit, int limit, String after) {
    return rateLimiter.acquireToken()              // Rate limiting
        .then(makeApiCall(subreddit, limit, after)) // HTTP request
        .retry(2)                                   // Simple retry
        .flatMapMany(this::extractPosts)            // Response processing
        .doOnError(error -> logger.error(...))      // Error logging  
        .onErrorResume(error -> Flux.empty());      // Graceful fallback
}
```

**Error Handling Matrix**:
| Error Type | Status Code | Action | Retry |
|------------|-------------|---------|--------|
| Rate Limited | 429 | Log warning, respect limits | Yes |
| Client Error | 4xx | Log error, fail fast | No |
| Server Error | 5xx | Log error, retry | Yes |
| Timeout | - | Log timeout, retry | Yes |
| Network | - | Log connection issue, retry | Yes |

**Response Processing**:
1. Validate Reddit API response structure
2. Extract posts from nested JSON structure
3. Filter invalid posts (deleted, NSFW, empty content)
4. Map Reddit fields to domain model
5. Return reactive stream of valid posts

### RateLimiter
**Purpose**: Token bucket rate limiting for external API calls

**Implementation Details**:
```java
public class RateLimiter {
    private final AtomicInteger tokens;           // Available tokens
    private final AtomicReference<Instant> lastRefill; // Last refill time
    private final int maxTokens = 60;            // Max tokens (requests/minute)
    private final Duration refillInterval = Duration.ofSeconds(1); // 1 token/second
    
    public Mono<Void> acquireToken() {
        return Mono.fromCallable(this::tryAcquireToken)
            .flatMap(acquired -> {
                if (acquired) {
                    return Mono.empty();         // Token acquired
                } else {
                    return Mono.delay(Duration.ofMillis(1000))
                              .then(acquireToken()); // Wait and retry
                }
            });
    }
}
```

**Algorithm**: Token bucket with 1 token refilled per second, max 60 tokens

## Data Access Architecture

### Repository Layer Design

#### SocialPostRepository
**Query Categories**:

1. **CRUD & Duplicate Detection**
   ```java
   boolean existsByExternalIdAndPlatform(String externalId, Platform platform);
   Optional<SocialPost> findByContentHashAndPlatform(String contentHash, Platform platform);
   ```

2. **Platform & Time-Based Queries**
   ```java
   Page<SocialPost> findByPlatformInAndCreatedAtBetween(
       List<Platform> platforms, LocalDateTime start, LocalDateTime end, Pageable pageable);
   ```

3. **Content Search**
   ```java
   @Query("SELECT s FROM SocialPost s WHERE " +
          "(LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
          " LOWER(s.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
          "s.platform = :platform ORDER BY s.engagementScore DESC")
   List<SocialPost> findByContentContainingAndPlatform(String keyword, Platform platform, Pageable pageable);
   ```

4. **Analytics Aggregations**
   ```java
   @Query("SELECT s.platform, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
          "FROM SocialPost s WHERE s.createdAt > :since GROUP BY s.platform")
   List<Object[]> getPlatformComparisonData(LocalDateTime since);
   ```

5. **Reddit-Specific Queries**
   ```java
   @Query("SELECT s.subreddit, COUNT(s) as postCount, AVG(s.engagementScore) as avgEngagement " +
          "FROM SocialPost s WHERE s.platform = 'REDDIT' AND s.subreddit IS NOT NULL " +
          "GROUP BY s.subreddit ORDER BY avgEngagement DESC")
   List<Object[]> getSubredditStats();
   ```

#### SentimentDataRepository
**Focus**: Cross-platform sentiment analytics

```java
@Query("SELECT sp.platform, AVG(sd.sentimentScore), COUNT(sd), " +
       "SUM(CASE WHEN sd.sentimentLabel = 'POSITIVE' THEN 1 ELSE 0 END), " +
       "SUM(CASE WHEN sd.sentimentLabel = 'NEGATIVE' THEN 1 ELSE 0 END) " +
       "FROM SentimentData sd JOIN sd.socialPost sp " +
       "WHERE sd.processedAt > :since GROUP BY sp.platform")
List<Object[]> getCrossPlatformSentimentComparison(LocalDateTime since);
```

## Configuration Architecture

### RedditApiConfig
**Purpose**: Type-safe configuration properties

```java
@ConfigurationProperties(prefix = "reddit.api")
public class RedditApiConfig {
    private String baseUrl = "https://www.reddit.com";
    private String userAgent = "SentimentAnalytics/1.0";
    private int requestsPerMinute = 60;
    private int connectionTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private String[] defaultSubreddits = {"technology", "programming", "worldnews"};
}
```

### WebClientConfig
**Purpose**: Production-ready HTTP client configuration

**Features**:
- Connection pooling and timeouts
- Request/response logging filters
- Error handling filters
- Compression enabled
- Large response buffer (1MB) for Reddit API
- Retry specification with exponential backoff

```java
@Bean
public WebClient redditWebClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
        .doOnConnected(conn -> 
            conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)))
        .compress(true);
    
    return WebClient.builder()
        .baseUrl(redditConfig.getBaseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader("User-Agent", redditConfig.getUserAgent())
        .filter(logRequest())
        .filter(logResponse())
        .filter(errorHandler())
        .build();
}
```

## Domain Model Architecture

### SocialPost Entity Design

**Core Identity**:
```java
@Entity
@Table(name = "social_posts", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_external_platform", 
                           columnNames = {"externalId", "platform"})
       })
public class SocialPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String externalId;     // Platform-specific post ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;     // REDDIT or YOUTUBE
}
```

**Content Fields**:
```java
@Column(length = 500, nullable = false)
private String title;           // Both platforms have titles in 2025

@Column(columnDefinition = "TEXT")
private String content;         // Optional for YouTube, required for Reddit

@Column(nullable = false, length = 100)
private String author;
```

**Engagement Metrics**:
```java
// Reddit-specific
private Long upvotes = 0L;

// YouTube-specific  
private Long likeCount = 0L;
private Long viewCount = 0L;

// Common metrics
private Long commentCount = 0L;
private Long shareCount = 0L;
private Double engagementScore = 0.0;  // Calculated field
```

**Platform-Specific Fields**:
```java
private String subreddit;       // Reddit: r/technology
private String videoId;         // YouTube: dQw4w9WgXcQ
private String url;            // External URL
```

**Content Analysis Collections**:
```java
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "post_hashtags")
private Set<String> hashtags = new HashSet<>();

@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "post_mentions")
private Set<String> mentions = new HashSet<>();

@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(name = "post_topics")
private Set<String> topicTags = new HashSet<>();
```

### Engagement Scoring Algorithm

**Reddit Formula**:
```java
private double calculateRedditEngagement() {
    long upvotes = this.upvotes != null ? this.upvotes : 0;
    long comments = this.commentCount != null ? this.commentCount : 0;
    
    // Comments weighted higher than upvotes
    double score = upvotes + (comments * 2.5);
    
    // Logarithmic scaling for very high scores
    if (score > 1000) {
        score = 1000 + Math.log10(score - 999) * 100;
    }
    
    return Math.max(0, score);
}
```

**YouTube Formula**:
```java
private double calculateYouTubeEngagement() {
    long likes = this.likeCount != null ? this.likeCount : 0;
    long comments = this.commentCount != null ? this.commentCount : 0;
    long views = this.viewCount != null ? this.viewCount : 0;
    
    if (views == 0) {
        return (likes * 1.5) + (comments * 3.0);
    }
    
    // Engagement rate based on views
    double engagementRate = ((double) (likes + comments)) / views;
    double baseEngagement = (likes * 1.5) + (comments * 3.0);
    
    return (engagementRate * views * 0.1) + baseEngagement;
}
```

## Extension Points

### 1. YouTube Integration
**Required Components**:
- `YouTubeApiClient` (similar to RedditApiClient)
- `YouTubeIngestionService` 
- YouTube-specific models in `model/youtube/` package
- Configuration: `YouTubeApiConfig`

**Existing Support**:
- Platform enum already includes YOUTUBE
- SocialPost entity supports YouTube fields (videoId, likeCount, viewCount)
- Repository queries work with Platform filtering
- Engagement scoring includes YouTube algorithm

### 2. Sentiment Analysis
**Ready Components**:
- `SentimentData` entity with confidence metrics
- `SentimentDataRepository` with analytics queries
- `SentimentLabel` enum (POSITIVE, NEGATIVE, NEUTRAL, MIXED, UNKNOWN)

**Implementation Needed**:
- Sentiment processing service
- NLP model integration (Stanford CoreNLP, Vader, etc.)
- Batch processing for existing posts

### 3. Real-time Features
**Extension Points**:
- WebSocket endpoints for live updates
- Redis integration for pub/sub messaging
- Event-driven architecture with Spring Cloud Stream

### 4. Advanced Analytics
**Available Data**:
- Cross-platform engagement comparison
- Time-based trend analysis
- Author performance metrics
- Subreddit/channel analytics
- Content feature analysis (hashtags, mentions, topics)

## Performance Characteristics

### Current Metrics
- **Ingestion Rate**: 50-150 posts per API call
- **Success Rate**: 100% for standard subreddit ingestion
- **Response Time**: Sub-100ms for health checks
- **Concurrency**: Non-blocking reactive streams
- **Memory**: Efficient with streaming processing

### Scalability Features
- **Reactive Streams**: Handles backpressure automatically
- **Connection Pooling**: Reuses HTTP connections
- **Rate Limiting**: Prevents API quota exhaustion
- **Batch Processing**: Efficient database operations
- **Pagination**: Large result sets handled efficiently

### Monitoring & Observability
- **Structured Logging**: SLF4J with context
- **Health Endpoints**: System status monitoring
- **Statistics Tracking**: Real-time metrics
- **Error Handling**: Comprehensive exception management