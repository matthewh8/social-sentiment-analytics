# Service Architecture Documentation - Analytics Dashboard MVP

## Overview
MVP-focused **social media analytics dashboard** with clean, simple architecture. Core functionality for Reddit data ingestion, engagement analytics, and insights generation.

## Service Layer Structure

```
services/
├── DataProcessingService.java      # Core analytics and data processing
├── RedditIngestionService.java     # Reddit data ingestion
├── RedditApiClient.java           # Reddit API communication
└── RateLimiter.java              # API rate limiting
```

## Core Services

### DataProcessingService
**Purpose**: Central business logic for analytics and data processing

**Key Responsibilities**:
- Process and validate ingested social posts
- Calculate engagement scores
- Generate analytics reports
- Search and filter existing posts
- Platform-specific engagement formulas

**Key Methods**:
```java
// Data Processing (for ingestion)
SocialPostDto processSocialPost(SocialPostDto postDto)
List<SocialPostDto> processSocialPosts(List<SocialPostDto> postDtos)

// Analytics & Search
Page<SocialPostDto> searchPosts(PostSearchCriteria criteria)
AnalyticsReport generateAnalyticsReport(LocalDateTime start, LocalDateTime end)
EngagementStats getEngagementStats(Platform platform, LocalDateTime start, LocalDateTime end)
List<SocialPostDto> getPostsByPlatform(Platform platform, int limit)

// Updates (for real-time data)
void updateEngagementMetrics(String externalId, Platform platform, SocialPostDto updates)
```

**Engagement Score Calculation**:
- **Reddit**: `upvotes + (comments * 2)` normalized to 0-100
- **Twitter**: `likes + (shares * 3) + (comments * 2)` normalized to 0-100
- **YouTube**: Engagement rate based on `(likes + comments) / views * 100`

### RedditIngestionService  
**Purpose**: Handle Reddit-specific data ingestion

**Key Responsibilities**:
- Fetch posts from subreddits
- Convert Reddit API responses to SocialPost entities
- Batch processing across multiple subreddits
- Handle empty content from link posts
- Simple duplicate filtering

**Key Methods**:
```java
// Ingestion
Mono<Integer> ingestFromSubreddit(String subreddit, int limit)
Mono<Integer> ingestFromMultipleSubreddits(List<String> subreddits, int limitPerSubreddit)
Mono<Integer> testIngestion()

// Statistics
IngestionStats getIngestionStats()
```

### RedditApiClient
**Purpose**: Handle Reddit API communication with rate limiting

**Key Responsibilities**:
- Make HTTP calls to Reddit API
- Handle rate limiting and retries
- Parse Reddit JSON responses
- Basic error handling and logging

**Key Methods**:
```java
// API calls
Flux<RedditPost> fetchSubredditPosts(String subreddit, int limit, String after)
Flux<RedditPost> fetchMultipleSubreddits(List<String> subreddits, int limitPerSubreddit)
```

### RateLimiter
**Purpose**: Token bucket rate limiting for API calls

**Configuration**: 60 requests per minute (configurable)

**Key Methods**:
```java
Mono<Void> acquireToken()           // Reactive token acquisition
boolean tryAcquireToken()           // Non-blocking attempt
int getAvailableTokens()            // For monitoring
```

## Controllers

### PostQueryController (Analytics Focus)
**Purpose**: REST API for querying and analyzing posts

**Endpoints**:
```
GET  /api/posts              # Search and filter posts
GET  /api/posts/{id}         # Get specific post details
GET  /api/posts/platform/{platform}  # Get posts by platform
GET  /api/posts/health       # Health check
```

### AnalyticsController (New)
**Purpose**: REST API for analytics and insights

**Endpoints**:
```
GET  /api/analytics/report       # Generate analytics reports
GET  /api/analytics/sentiment    # Sentiment analysis
GET  /api/analytics/engagement   # Engagement metrics
GET  /api/analytics/trends       # Trending topics/posts
```

### RedditController
**Purpose**: REST API for Reddit ingestion operations

**Endpoints**:
```
POST /api/reddit/ingest   # Trigger manual ingestion
POST /api/reddit/test     # Test ingestion
GET  /api/reddit/stats    # Get ingestion statistics
GET  /api/reddit/health   # Health check
```

## Data Transfer Objects (DTOs)

### SocialPostDto
**Core Fields**:
- `id`, `externalId`, `platform`, `title`, `content`, `author`
- `createdAt`, `ingestedAt`

**Engagement Metrics**:
- `likeCount`, `shareCount`, `commentCount`, `upvotes`, `viewCount`
- `engagementScore` (calculated 0-100)

**Platform-Specific**:
- `subreddit` (Reddit)
- `videoId` (YouTube)

**Sentiment Data**:
- `sentimentLabel`, `sentimentScore`, `confidence`

### PostSearchCriteria
**Search Options**:
- Platform filtering
- Date range filtering
- Content keyword search
- Author filtering
- Basic engagement filtering
- Sentiment filtering
- Pagination and sorting

### EngagementStats
**Metrics**:
- Average, min, max engagement scores
- Total likes, upvotes, comments, shares, views
- Basic engagement calculations

### AnalyticsReport
**Report Data**:
- Overall metrics (total posts, authors, avg engagement)
- Platform breakdown
- Sentiment distribution
- Top authors and posts
- Reddit subreddit statistics

## Data Flow

### Reddit Ingestion Flow (Primary Data Source)
```
1. RedditController.triggerIngestion()
   ↓
2. RedditIngestionService.ingestFromMultipleSubreddits()
   ↓
3. RedditApiClient.fetchSubredditPosts()
   ↓
4. RateLimiter.acquireToken()
   ↓
5. HTTP call to Reddit API
   ↓
6. Convert RedditPost to SocialPostDto
   ↓
7. DataProcessingService.processSocialPost()
   ↓
8. Calculate engagement score
   ↓
9. Save to database
```

### Analytics Generation Flow
```
1. AnalyticsController.generateReport()
   ↓
2. DataProcessingService.generateAnalyticsReport()
   ↓
3. Multiple repository queries:
   - Volume statistics
   - Platform comparisons
   - Top authors/posts
   - Sentiment distribution
   ↓
4. Aggregate data into AnalyticsReport
   ↓
5. Return to frontend dashboard
```

### Post Query Flow
```
1. PostQueryController.searchPosts()
   ↓
2. DataProcessingService.searchPosts()
   ↓
3. Repository query with criteria
   ↓
4. Convert entities to DTOs
   ↓
5. Return paginated results
```

## Repository Layer

### SocialPostRepository
**Key Queries**:
- `findByExternalIdAndPlatform()` - Duplicate checking
- `findByPlatform()` - Platform filtering
- `findByPlatformAndCreatedAtAfter()` - Recent posts
- `existsByExternalIdAndPlatform()` - Duplicate validation
- Search and analytics queries

### SentimentDataRepository
**Key Queries**:
- `findBySocialPostId()` - Get sentiment for post
- `findByPostExternalIdAndPlatform()` - Platform-specific sentiment
- Sentiment analytics queries

## Configuration

### Required Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop

# Reddit API
reddit.api.base-url=https://www.reddit.com
reddit.api.user-agent=SocialMediaAnalytics/1.0

# Rate Limiting
rate.limiter.tokens-per-minute=60
```

## Error Handling

### Custom Exceptions
- `DuplicatePostException` - When trying to save duplicate posts
- `PostNotFoundException` - When post doesn't exist for update
- `ValidationException` - Input validation failures
- `AnalyticsException` - Analytics generation errors

### Error Response Format
```json
{
  "status": "error",
  "message": "Error description",
  "timestamp": 1692276000000
}
```

## API Usage Examples

### Get Posts
```bash
curl "http://localhost:8080/api/posts?platform=REDDIT&size=10"
```

### Generate Analytics Report
```bash
curl "http://localhost:8080/api/analytics/report?startDate=2025-08-16&endDate=2025-08-17"
```

### Trigger Reddit Ingestion
```bash
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology,programming&postsPerSubreddit=10"
```

### Get Sentiment Analysis
```bash
curl "http://localhost:8080/api/analytics/sentiment?platform=REDDIT"
```

## Performance Considerations

### Current Optimizations
- Rate limiting for Reddit API (60 requests/minute)
- Duplicate detection before saving
- Reactive streams for concurrent processing
- Simple engagement calculations

### Monitoring Points
- Posts ingested per hour
- API rate limit usage
- Database response times
- Analytics generation performance

This architecture provides a solid foundation for the analytics dashboard MVP while maintaining clean separation of concerns and scalability for future platform integrations.