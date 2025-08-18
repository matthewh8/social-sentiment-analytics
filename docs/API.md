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

## Future API Endpoints

The following endpoints are designed and ready for implementation:

### Post Management

#### Save Social Post
```http
POST /api/posts
Content-Type: application/json
```

**Request Body**:
```json
{
  "externalId": "abc123",
  "platform": "REDDIT",
  "title": "Amazing Tech Discovery",
  "content": "This post discusses...",
  "author": "techguru42",
  "createdAt": "2024-08-18T10:30:00",
  "subreddit": "technology",
  "upvotes": 156,
  "commentCount": 23,
  "url": "https://reddit.com/r/technology/..."
}
```

**Response** (`201`):
```json
{
  "id": 1001,
  "externalId": "abc123",
  "platform": "REDDIT",
  "title": "Amazing Tech Discovery",
  "content": "This post discusses...",
  "author": "techguru42",
  "createdAt": "2024-08-18T10:30:00",
  "ingestedAt": "2024-08-18T15:45:00",
  "subreddit": "technology",
  "upvotes": 156,
  "commentCount": 23,
  "engagementScore": 213.5,
  "url": "https://reddit.com/r/technology/..."
}
```

#### Advanced Post Search
```http
POST /api/posts/search
Content-Type: application/json
```

**Request Body**:
```json
{
  "platforms": ["REDDIT"],
  "startDate": "2024-08-01T00:00:00",
  "endDate": "2024-08-18T23:59:59",
  "contentKeyword": "artificial intelligence",
  "subreddits": ["technology", "MachineLearning"],
  "minEngagementScore": 100.0,
  "sentimentLabels": ["POSITIVE"],
  "page": 0,
  "size": 20,
  "sortBy": "engagementScore",
  "sortDirection": "DESC"
}
```

**Response** (`200`):
```json
{
  "content": [
    {
      "id": 1001,
      "externalId": "abc123",
      "platform": "REDDIT",
      "title": "AI Breakthrough in 2024",
      "author": "ai_researcher",
      "subreddit": "MachineLearning",
      "engagementScore": 450.0,
      "sentimentLabel": "POSITIVE",
      "sentimentScore": 0.85
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "properties": ["engagementScore"]
    }
  },
  "totalElements": 156,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### Analytics API

#### Generate Analytics Report
```http
POST /api/analytics/report
Content-Type: application/json
```

**Request Body**:
```json
{
  "startDate": "2024-08-01T00:00:00",
  "endDate": "2024-08-18T23:59:59",
  "platforms": ["REDDIT"],
  "includeSubredditStats": true,
  "includeSentimentTrends": true
}
```

**Response** (`200`):
```json
{
  "generatedAt": "2024-08-18T16:00:00",
  "periodStart": "2024-08-01T00:00:00",
  "periodEnd": "2024-08-18T23:59:59",
  "totalPosts": 1247,
  "totalAuthors": 892,
  "averageEngagementScore": 156.7,
  "postsByPlatform": {
    "REDDIT": 1247
  },
  "avgEngagementByPlatform": {
    "REDDIT": 156.7
  },
  "sentimentDistribution": {
    "POSITIVE": 456,
    "NEUTRAL": 623,
    "NEGATIVE": 168
  },
  "overallSentimentScore": 0.62,
  "topAuthors": [
    {
      "username": "techguru42",
      "postCount": 15,
      "avgEngagementScore": 234.5,
      "primaryPlatform": "REDDIT"
    }
  ],
  "topPosts": [
    {
      "id": 1001,
      "title": "Revolutionary AI Discovery",
      "author": "ai_researcher",
      "platform": "REDDIT",
      "engagementScore": 890.5,
      "sentiment": "POSITIVE"
    }
  ],
  "topSubreddits": [
    {
      "subreddit": "technology",
      "postCount": 234,
      "avgEngagementScore": 178.9
    }
  ]
}
```

#### Get Engagement Statistics
```http
GET /api/analytics/engagement
```

**Query Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `platform` | string | No | Filter by platform (REDDIT, YOUTUBE) |
| `startDate` | string | Yes | Start date (ISO 8601 format) |
| `endDate` | string | Yes | End date (ISO 8601 format) |

**Example Request**:
```bash
curl "http://localhost:8080/api/analytics/engagement?platform=REDDIT&startDate=2024-08-01T00:00:00&endDate=2024-08-18T23:59:59"
```

**Response** (`200`):
```json
{
  "averageEngagementScore": 156.7,
  "medianEngagementScore": 89.5,
  "maxEngagementScore": 2340.0,
  "minEngagementScore": 0.0,
  "totalUpvotes": 145670,
  "totalComments": 23456,
  "averageUpvotes": 116.8,
  "averageComments": 18.8,
  "engagementScore25thPercentile": 23.5,
  "engagementScore75thPercentile": 234.7,
  "engagementScore90thPercentile": 456.2
}
```

#### Find Duplicate Posts
```http
GET /api/analytics/duplicates
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `platform` | string | Required | Platform to check (REDDIT, YOUTUBE) |
| `days` | integer | `7` | Number of days to look back |

**Example Request**:
```bash
curl "http://localhost:8080/api/analytics/duplicates?platform=REDDIT&days=3"
```

**Response** (`200`):
```json
{
  "duplicatesFound": 12,
  "duplicatePosts": [
    {
      "id": 1001,
      "externalId": "duplicate1",
      "title": "Same Title Posted Multiple Times",
      "author": "author1",
      "subreddit": "technology",
      "createdAt": "2024-08-18T10:00:00"
    }
  ]
}
```

### Sentiment Analysis API

#### Get Sentiment by Platform
```http
GET /api/sentiment/platform/{platform}
```

**Path Parameters**:
- `platform`: REDDIT or YOUTUBE

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `startDate` | string | 30 days ago | Start date for analysis |
| `endDate` | string | now | End date for analysis |

**Response** (`200`):
```json
{
  "platform": "REDDIT",
  "totalAnalyzed": 1247,
  "averageSentimentScore": 0.62,
  "sentimentDistribution": {
    "POSITIVE": 456,
    "NEUTRAL": 623,
    "NEGATIVE": 168
  },
  "topPositiveSubreddits": [
    {
      "subreddit": "UpliftingNews",
      "avgSentimentScore": 0.82,
      "postCount": 45
    },
    {
      "subreddit": "technology",
      "avgSentimentScore": 0.68,
      "postCount": 234
    }
  ],
  "confidenceStats": {
    "averageConfidence": 0.87,
    "highConfidencePosts": 1089,
    "lowConfidencePosts": 158
  }
}
```

#### Cross-Platform Sentiment Comparison
```http
GET /api/sentiment/cross-platform
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `startDate` | string | 30 days ago | Start date for comparison |
| `endDate` | string | now | End date for comparison |

**Response** (`200`):
```json
{
  "comparisonPeriod": {
    "startDate": "2024-07-19T00:00:00",
    "endDate": "2024-08-18T23:59:59"
  },
  "platformComparison": [
    {
      "platform": "REDDIT",
      "totalPosts": 1247,
      "avgSentimentScore": 0.62,
      "positivePercentage": 36.6,
      "negativePercentage": 13.5,
      "neutralPercentage": 49.9
    },
    {
      "platform": "YOUTUBE",
      "totalPosts": 0,
      "avgSentimentScore": null,
      "positivePercentage": 0,
      "negativePercentage": 0,
      "neutralPercentage": 0
    }
  ],
  "overallTrends": {
    "mostPositivePlatform": "REDDIT",
    "mostNegativePlatform": null,
    "sentimentVolatility": 0.23
  }
}
```

## Error Handling

### Standard Error Response Format
All API endpoints return errors in a consistent format:

```json
{
  "timestamp": "2024-08-18T16:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: Title cannot be null",
  "path": "/api/posts"
}
```

### Common HTTP Status Codes

| Status Code | Description | When Used |
|-------------|-------------|-----------|
| `200` | OK | Successful GET/PUT requests |
| `201` | Created | Successful POST requests |
| `400` | Bad Request | Invalid request parameters or body |
| `404` | Not Found | Resource not found |
| `409` | Conflict | Duplicate post attempt |
| `422` | Unprocessable Entity | Validation errors |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Unexpected server errors |
| `503` | Service Unavailable | Service is down or overloaded |

### Specific Error Types

#### Validation Errors (`400`)
```json
{
  "timestamp": "2024-08-18T16:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Validation failed: Title must not exceed 500 characters, Platform is required",
  "path": "/api/posts",
  "validationErrors": [
    {
      "field": "title",
      "message": "Title must not exceed 500 characters"
    },
    {
      "field": "platform",
      "message": "Platform is required"
    }
  ]
}
```

#### Duplicate Post Error (`409`)
```json
{
  "timestamp": "2024-08-18T16:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Post with external ID 'abc123' already exists for platform REDDIT",
  "path": "/api/posts",
  "duplicatePost": {
    "externalId": "abc123",
    "platform": "REDDIT",
    "existingId": 1001
  }
}
```

#### Rate Limit Error (`429`)
```json
{
  "timestamp": "2024-08-18T16:00:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Maximum 60 requests per minute allowed.",
  "path": "/api/reddit/ingest",
  "retryAfter": 30
}
```

## Rate Limiting

### Current Limits
- **Reddit API**: 60 requests per minute
- **Manual Ingestion**: No limit (controlled by Reddit API limit)
- **Search API**: 100 requests per minute (planned)
- **Analytics API**: 20 requests per minute (planned)

### Rate Limit Headers
All responses include rate limiting information:

```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1692345660
```

## Authentication & Authorization

### Current State
- **Development**: No authentication required
- **Production**: JWT-based authentication planned

### Planned Authentication
```http
Authorization: Bearer <jwt_token>
```

**Token Structure**:
```json
{
  "sub": "user123",
  "roles": ["USER", "ANALYST"],
  "exp": 1692345600,
  "iat": 1692342000
}
```

## Request/Response Examples

### Complete Reddit Ingestion Workflow

#### 1. Check Service Health
```bash
curl -X GET "http://localhost:8080/api/reddit/health"
```

#### 2. Trigger Ingestion
```bash
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology,programming&postsPerSubreddit=25"
```

#### 3. Monitor Statistics
```bash
curl -X GET "http://localhost:8080/api/reddit/stats"
```

#### 4. Search Ingested Posts
```bash
curl -X POST "http://localhost:8080/api/posts/search" \
  -H "Content-Type: application/json" \
  -d '{
    "platforms": ["REDDIT"],
    "contentKeyword": "artificial intelligence",
    "minEngagementScore": 50.0,
    "page": 0,
    "size": 10
  }'
```

### Bulk Operations

#### Batch Post Creation
```bash
curl -X POST "http://localhost:8080/api/posts/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "posts": [
      {
        "externalId": "post1",
        "platform": "REDDIT",
        "title": "First Post",
        "content": "Content of first post",
        "author": "author1",
        "subreddit": "technology"
      },
      {
        "externalId": "post2",
        "platform": "REDDIT", 
        "title": "Second Post",
        "content": "Content of second post",
        "author": "author2",
        "subreddit": "programming"
      }
    ]
  }'
```

**Response**:
```json
{
  "totalProcessed": 2,
  "successful": 2,
  "failed": 0,
  "results": [
    {
      "externalId": "post1",
      "status": "SUCCESS",
      "id": 1001
    },
    {
      "externalId": "post2", 
      "status": "SUCCESS",
      "id": 1002
    }
  ]
}
```

## API Versioning

### Current Version
- **Version**: v1
- **Base Path**: `/api/v1` (planned for production)
- **Development**: `/api` (no versioning)

### Version Headers
```http
Accept: application/vnd.socialmedia.v1+json
API-Version: 1.0
```

## Data Export API

### Export Posts
```http
GET /api/export/posts
```

**Query Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `format` | string | Export format (json, csv, xlsx) |
| `platform` | string | Filter by platform |
| `startDate` | string | Start date filter |
| `endDate` | string | End date filter |
| `includeContent` | boolean | Include full post content |

**Example**:
```bash
curl "http://localhost:8080/api/export/posts?format=csv&platform=REDDIT&startDate=2024-08-01&includeContent=false" \
  -H "Accept: text/csv" \
  -o reddit_posts.csv
```

### Export Analytics
```http
GET /api/export/analytics
```

**Query Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `format` | string | Export format (json, xlsx) |
| `reportType` | string | Type of analytics (engagement, sentiment, trends) |
| `startDate` | string | Start date filter |
| `endDate` | string | End date filter |

## WebSocket API (Planned)

### Real-time Ingestion Updates
```javascript
// Connect to WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/ingestion');

// Subscribe to ingestion events
ws.send(JSON.stringify({
  type: 'SUBSCRIBE',
  channel: 'reddit.ingestion'
}));

// Receive real-time updates
ws.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('New posts ingested:', data.postsIngested);
};
```

### Live Analytics Dashboard
```javascript
// Subscribe to analytics updates
ws.send(JSON.stringify({
  type: 'SUBSCRIBE', 
  channel: 'analytics.live'
}));

// Receive live metrics
ws.onmessage = function(event) {
  const metrics = JSON.parse(event.data);
  updateDashboard(metrics);
};
```

## API Testing

### Health Check Test Suite
```bash
#!/bin/bash
# Test all health endpoints

echo "Testing Reddit API health..."
curl -s "http://localhost:8080/api/reddit/health" | jq '.status'

echo "Testing Data Processing API health..."
curl -s "http://localhost:8080/api/posts/health" | jq '.status'

echo "Testing database connectivity..."
curl -s "http://localhost:8080/api/reddit/stats" | jq '.status'
```

### Load Testing
```bash
# Simple load test for ingestion endpoint
for i in {1..10}; do
  curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=programming&postsPerSubreddit=5" &
done
wait
```

### Integration Test Examples
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedditControllerIntegrationTest {
    
    @Test
    void testHealthEndpoint() {
        ResponseEntity<Map> response = testRestTemplate.getForEntity(
            "/api/reddit/health", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
    
    @Test
    void testIngestionEndpoint() {
        ResponseEntity<Map> response = testRestTemplate.postForEntity(
            "/api/reddit/ingest?subreddits=programming&postsPerSubreddit=5", 
            null, Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("postsIngested")).isNotNull();
    }
}
```

## API Monitoring

### Metrics Endpoints
```http
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/reddit.api.calls
GET /actuator/metrics/database.connections
```

### Health Indicators
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "SELECT 1"
      }
    },
    "reddit-api": {
      "status": "UP",
      "details": {
        "baseUrl": "https://www.reddit.com",
        "lastSuccessfulCall": "2024-08-18T15:30:00"
      }
    }
  }
}
```

This comprehensive API documentation provides all the information needed to interact with the Social Media Sentiment Analytics System, both for current Reddit functionality and planned future features.