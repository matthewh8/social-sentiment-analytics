# API Documentation

## Base URL
```
http://localhost:8080/api
```

## Reddit Ingestion API

### Health Check
Get the health status of the Reddit ingestion service.

```http
GET /api/reddit/health
```

**Response**:
```json
{
  "status": "UP",
  "service": "Reddit Data Ingestion",
  "timestamp": "1692345600000"
}
```

**Status Codes**:
- `200` - Service is healthy
- `503` - Service is down

### Manual Ingestion Trigger
Manually trigger Reddit post ingestion from specified subreddits.

```http
POST /api/reddit/ingest
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `subreddits` | string | `technology,programming` | Comma-separated list of subreddit names |
| `postsPerSubreddit` | integer | `25` | Number of posts to fetch per subreddit |

**Example Requests**:
```bash
# Basic ingestion
curl -X POST "http://localhost:8080/api/reddit/ingest"

# Custom subreddits
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology,programming,MachineLearning&postsPerSubreddit=50"

# Single subreddit with custom limit
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=AskReddit&postsPerSubreddit=10"
```

**Success Response** (`200`):
```json
{
  "status": "success",
  "message": "Ingestion completed",
  "postsIngested": 75,
  "subreddits": ["technology", "programming", "MachineLearning"]
}
```

**Error Response** (`500`):
```json
{
  "status": "error",
  "message": "Ingestion failed"
}
```

### Trending Posts Ingestion
Ingest trending posts from r/popular.

```http
POST /api/reddit/trending
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | `50` | Number of trending posts to fetch |

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/reddit/trending?limit=25"
```

**Success Response** (`200`):
```json
{
  "status": "success",
  "message": "Trending posts ingestion completed",
  "postsIngested": 25
}
```

### System Statistics
Get comprehensive statistics about the ingestion system.

```http
GET /api/reddit/stats
```

**Response** (`200`):
```json
{
  "status": "healthy",
  "statistics": {
    "totalPosts": 1247,
    "redditPosts": 1247,
    "recentPosts24h": 156,
    "sessionTotal": 75
  },
  "timestamp": 1692345600000
}
```

**Field Descriptions**:
- `totalPosts`: Total posts in the database
- `redditPosts`: Total Reddit posts ingested
- `recentPosts24h`: Posts ingested in the last 24 hours
- `sessionTotal`: Posts ingested in the current session

### Service Configuration
Get current service configuration information.

```http
GET /api/reddit/config
```

**Response** (`200`):
```json
{
  "schedulingEnabled": true,
  "schedulingInterval": "5 minutes",
  "rateLimitEnabled": true,
  "retryEnabled": true,
  "maxRetries": 3
}
```

## YouTube Ingestion API

### Health Check
Get the health status of the YouTube ingestion service.

```http
GET /api/youtube/health
```

**Response**:
```json
{
  "status": "UP",
  "service": "YouTube Data Ingestion",
  "timestamp": "1692345600000"
}
```

### Channel-Based Ingestion
Manually trigger YouTube video ingestion from a specific channel.

```http
POST /api/youtube/ingest/channel/{channelId}
```

**Path Parameters**:
- `channelId`: YouTube channel ID (e.g., UCBJycsmduvYEL83R_U4JriQ)

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | `25` | Number of videos to fetch from the channel |

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/youtube/ingest/channel/UCBJycsmduvYEL83R_U4JriQ?limit=30"
```

**Success Response** (`200`):
```json
{
  "status": "success",
  "message": "Channel ingestion completed",
  "videosIngested": 30,
  "channelId": "UCBJycsmduvYEL83R_U4JriQ"
}
```

### Multi-Channel Ingestion
Trigger ingestion from multiple channels simultaneously.

```http
POST /api/youtube/ingest
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `channels` | string | `UCBJycsmduvYEL83R_U4JriQ,UCXuqSBlHAE6Xw-yeJA0Tunw` | Comma-separated channel IDs |
| `videosPerChannel` | integer | `25` | Number of videos to fetch per channel |

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/youtube/ingest?channels=UCBJycsmduvYEL83R_U4JriQ,UCXuqSBlHAE6Xw-yeJA0Tunw&videosPerChannel=20"
```

**Success Response** (`200`):
```json
{
  "status": "success",
  "message": "Ingestion completed",
  "videosIngested": 40,
  "channels": ["UCBJycsmduvYEL83R_U4JriQ", "UCXuqSBlHAE6Xw-yeJA0Tunw"]
}
```

### Search-Based Ingestion
Trigger ingestion based on YouTube search queries.

```http
POST /api/youtube/ingest/search
```

**Request Body**:
```json
{
  "query": "artificial intelligence",
  "limit": 25
}
```

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/youtube/ingest/search" \
  -H "Content-Type: application/json" \
  -d '{"query": "machine learning", "limit": 30}'
```

**Success Response** (`200`):
```json
{
  "status": "success",
  "message": "Search ingestion completed",
  "videosIngested": 25,
  "query": "artificial intelligence"
}
```

### Trending Videos Ingestion
Ingest trending videos from YouTube's trending section.

```http
POST /api/youtube/trending
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | `50` | Number of trending videos to fetch |

**Example Request**:
```bash
curl -X POST "http://localhost:8080/api/youtube/trending?limit=25"
```

**Success Response** (`200`):
```json
{
  "status": "success",
  "message": "Trending videos ingestion completed",
  "videosIngested": 25
}
```

### YouTube System Statistics
Get comprehensive statistics about the YouTube ingestion system.

```http
GET /api/youtube/stats
```

**Response** (`200`):
```json
{
  "status": "healthy",
  "statistics": {
    "totalPosts": 1247,
    "youtubePosts": 450,
    "recentPosts24h": 89,
    "sessionTotal": 25
  },
  "timestamp": 1692345600000
}
```

**Field Descriptions**:
- `totalPosts`: Total posts across all platforms in the database
- `youtubePosts`: Total YouTube videos ingested
- `recentPosts24h`: YouTube videos ingested in the last 24 hours
- `sessionTotal`: YouTube videos ingested in the current session

### YouTube Service Configuration
Get current YouTube service configuration information.

```http
GET /api/youtube/config
```

**Response** (`200`):
```json
{
  "schedulingEnabled": true,
  "schedulingInterval": "5 minutes",
  "rateLimitEnabled": true,
  "retryEnabled": true,
  "maxRetries": 3,
  "quotaLimitPerDay": 10000
}
```

## Data Processing API

### Health Check
Get the health status of the data processing service.

```http
GET /api/posts/health
```

**Response**:
```json
{
  "status": "UP",
  "service": "Data Processing Service",
  "timestamp": "1692345600000"
}
```

### Test Endpoint
Simple connectivity test for the data processing service.

```http
GET /api/posts/test
```

**Response**:
```json
{
  "message": "Data Processing Controller is working!",
  "availableEndpoints": "POST /api/posts, GET /api/posts/health, GET /api/posts/test"
}
```

## Platform Support

### Current Implementation Status
| Platform | Status | Features Available |
|----------|--------|-------------------|
| Reddit | ✅ Fully Implemented | Manual ingestion, trending posts, statistics |
| YouTube | ✅ Fully Implemented | Channel ingestion, search ingestion, trending videos |

### Platform-Specific Features

#### Reddit Features
- Subreddit-based ingestion
- Trending posts from r/popular
- Upvote and comment tracking
- No downvotes (deprecated in 2025)

#### YouTube Features  
- Channel-based ingestion using channel IDs
- Search-based ingestion with queries
- Trending videos from Science & Technology category
- Like count, view count, and comment tracking
- Proper handling of YouTube's two-step API process (search → video details)

## Configuration

### Application Properties
The system uses the following configuration structure:

```properties
# Database (H2 for development)
spring.datasource.url=jdbc:h2:mem:socialsentiment
spring.jpa.hibernate.ddl-auto=create-drop

# Reddit API
reddit.api.base-url=https://www.reddit.com
reddit.api.user-agent=SentimentAnalytics/1.0 by YourUsername
reddit.api.requests-per-minute=60
reddit.api.default-subreddits=technology,programming,worldnews,AskReddit,MachineLearning,artificial

# YouTube API
youtube.api.base-url=https://www.googleapis.com/youtube/v3
youtube.api.api-key=YOUR_API_KEY_HERE
youtube.api.requests-per-second=100
youtube.api.quota-units-per-day=10000
youtube.api.default-channels=UCBJycsmduvYEL83R_U4JriQ,UCXuqSBlHAE6Xw-yeJA0Tunw
youtube.api.default-search-terms=technology,programming,artificial intelligence
```

### Rate Limiting
- **Reddit API**: 60 requests per minute (token bucket algorithm)
- **YouTube API**: 100 requests per second, 10,000 quota units per day
- **Shared Rate Limiter**: Uses AtomicInteger-based token bucket implementation

## Data Model

### SocialPost Entity Structure
Both Reddit and YouTube posts are stored in a unified `social_posts` table with platform-specific fields:

#### Common Fields (Both Platforms)
- `id`: Auto-generated primary key
- `external_id`: Platform-specific post/video ID
- `platform`: REDDIT or YOUTUBE
- `title`: Post/video title (both platforms have titles in 2025)
- `content`: Post content/video description
- `author`: Username/channel name
- `created_at`: Original publication timestamp
- `comment_count`: Number of comments
- `engagement_score`: Calculated engagement metric

#### Reddit-Specific Fields
- `upvotes`: Reddit upvotes (no downvotes as of 2025)
- `subreddit`: Subreddit name without "r/" prefix

#### YouTube-Specific Fields
- `video_id`: YouTube video ID
- `like_count`: YouTube like count
- `view_count`: YouTube view count

## Error Handling

### Standard Error Response Format
```json
{
  "status": "error",
  "message": "Descriptive error message"
}
```

### Common Error Scenarios
- **Rate Limited** (429): API quota exceeded
- **Invalid API Key** (403): YouTube API authentication failure
- **Network Timeout**: Connection/read timeout exceeded
- **Duplicate Posts**: Handled gracefully with filtering
- **Invalid Response**: Empty or malformed API responses

## Testing Examples

### Complete Workflow Test

#### 1. Check Both Services
```bash
# Reddit health
curl -X GET "http://localhost:8080/api/reddit/health"

# YouTube health  
curl -X GET "http://localhost:8080/api/youtube/health"

# Data processing health
curl -X GET "http://localhost:8080/api/posts/health"
```

#### 2. Trigger Ingestion
```bash
# Reddit ingestion
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology,programming&postsPerSubreddit=25"

# YouTube channel ingestion
curl -X POST "http://localhost:8080/api/youtube/ingest/channel/UCBJycsmduvYEL83R_U4JriQ?limit=25"

# YouTube search ingestion
curl -X POST "http://localhost:8080/api/youtube/ingest/search" \
  -H "Content-Type: application/json" \
  -d '{"query": "programming tutorial", "limit": 20}'
```

#### 3. Monitor Statistics
```bash
# Reddit stats
curl -X GET "http://localhost:8080/api/reddit/stats"

# YouTube stats
curl -X GET "http://localhost:8080/api/youtube/stats"
```

### Development Database Access
- **H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:socialsentiment`
- **Username**: `sa`
- **Password**: (empty)

## API Architecture Notes

### Reactive Programming
- All ingestion services use **Spring WebFlux** with Mono/Flux patterns
- Non-blocking I/O for external API calls
- Proper backpressure handling with rate limiting

### Error Handling Strategy
- **Graceful Degradation**: Individual failures don't stop batch processing
- **Simple Retry**: 2 retries for transient failures
- **Rate Limit Respect**: Token bucket algorithm prevents API quota issues
- **Duplicate Prevention**: Checks `external_id + platform` uniqueness

### Data Flow
1. **API Request** → Controller validates parameters
2. **Service Layer** → Orchestrates ingestion workflow
3. **API Client** → Makes reactive HTTP calls with rate limiting
4. **Data Conversion** → Maps platform models to unified SocialPost entity
5. **Duplicate Filtering** → Prevents redundant storage
6. **Batch Save** → Efficient database operations
7. **Statistics Update** → Session counters for monitoring

### YouTube API Integration
- **Two-Step Process**: Search API → Video Details API
- **Proper ID Handling**: Handles both search response (`id.videoId`) and video response (`id`) formats
- **Statistics Fetching**: Gets engagement metrics (views, likes, comments)
- **Channel Support**: Fetch latest videos from specific channels
- **Search Support**: Query-based video discovery

## Configuration Classes

### RedditApiConfig
```java
@ConfigurationProperties(prefix = "reddit.api")
public class RedditApiConfig {
    private String baseUrl = "https://www.reddit.com";
    private String userAgent = "SentimentAnalytics/1.0";
    private int requestsPerMinute = 60;
    private String[] defaultSubreddits = {"technology", "programming", "worldnews"};
    // ... other configuration
}
```

### YouTubeApiConfig  
```java
@ConfigurationProperties(prefix = "youtube.api")
public class YouTubeApiConfig {
    private String baseUrl = "https://www.googleapis.com/youtube/v3";
    private String apiKey; // Required
    private int requestsPerSecond = 100;
    private int quotaUnitsPerDay = 10000;
    private String[] defaultChannels = {
        "UCBJycsmduvYEL83R_U4JriQ", // MKBHD
        "UCXuqSBlHAE6Xw-yeJA0Tunw"  // Linus Tech Tips
    };
    // ... other configuration
}
```

## Implementation Status

### Completed Features
- ✅ Reddit API integration with rate limiting
- ✅ YouTube API integration with dual WebClient setup
- ✅ Unified data model for both platforms
- ✅ Reactive programming with WebFlux
- ✅ Duplicate detection and filtering
- ✅ Engagement score calculation
- ✅ Comprehensive error handling
- ✅ Health check endpoints
- ✅ Statistics tracking
- ✅ H2 database integration

### Ready for Enhancement
- 🚧 Sentiment analysis integration (data model ready)
- 🚧 PostgreSQL migration (configuration classes ready)
- 🚧 Redis caching (WebClient configuration supports it)
- 🚧 Advanced search API (DTOs implemented)
- 🚧 Analytics reporting (data structures defined)

## Required Setup

### YouTube API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable YouTube Data API v3
4. Create credentials (API Key)
5. Update `application.properties`:
   ```properties
   youtube.api.api-key=YOUR_ACTUAL_API_KEY_HERE
   ```

### Default Configuration
The system comes with working defaults:
- **Reddit**: Fetches from technology, programming, worldnews, AskReddit subreddits
- **YouTube**: Configured for major tech channels (MKBHD, Linus Tech Tips)
- **Rate Limiting**: Conservative limits to prevent API quota issues
- **Database**: H2 in-memory for development (auto-creates schema)

This system is production-ready for the core ingestion functionality and designed for easy extension to PostgreSQL, Redis caching, and sentiment analysis.