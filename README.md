# Social Media Sentiment Analytics System

A distributed microservices platform for real-time social media sentiment analysis, currently supporting Reddit with planned YouTube integration.

## 🎯 Project Overview

**Goal**: Build a scalable system that ingests, processes, and analyzes social media posts for sentiment trends and engagement metrics.

**Current Status**: Reddit integration complete and operational ✅  
**Tech Stack**: Java 17, Spring Boot 3.5.4, Spring WebFlux, H2/PostgreSQL, Maven

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Running the Application
```bash
# Clone and navigate to the project
cd services/data.ingestion.service

# Run the application
./mvnw spring-boot:run

# Or with Maven
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

### Test the System
```bash
# Health check
curl http://localhost:8080/api/reddit/health

# Trigger Reddit ingestion
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology,programming&postsPerSubreddit=25"

# View statistics
curl http://localhost:8080/api/reddit/stats

# Access H2 database console
# Go to: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:socialsentiment
# Username: sa, Password: (empty)
```

## 📊 Current Capabilities

### ✅ Implemented
- **Reddit Data Ingestion**: Fetches posts from any subreddit
- **Reactive Architecture**: Uses Spring WebFlux for non-blocking operations
- **Rate Limiting**: Respects Reddit API limits (60 requests/minute)
- **Duplicate Detection**: Prevents ingesting the same post twice
- **Engagement Scoring**: Calculates platform-specific engagement metrics
- **Advanced Search**: Query posts by content, author, subreddit, date range
- **Real-time Statistics**: Track ingestion metrics and system health
- **Content Analysis**: Extracts hashtags, mentions, and topics

### 🔄 Ready for Implementation
- **Sentiment Analysis Pipeline**: Models and repositories are ready
- **Analytics Reports**: Comprehensive reporting system prepared
- **YouTube Integration**: Data models support YouTube posts
- **Cross-platform Analytics**: Compare sentiment across platforms

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Reddit API    │    │  Data Processing │    │   Analytics     │
│   Integration   │────│     Service      │────│    Reports      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Rate Limiter  │    │  Social Posts   │    │ Sentiment Data  │
│  (Token Bucket) │    │   Repository    │    │   Repository    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Core Components
- **RedditApiClient**: Reactive HTTP client for Reddit API
- **DataProcessingService**: Business logic for post processing
- **SocialPost**: Domain model supporting Reddit/YouTube
- **Rate Limiter**: Token bucket algorithm for API rate limiting

## 📁 Project Structure

```
src/main/java/com/socialmedia/data/ingestion/
├── config/                 # Configuration classes
│   ├── RedditApiConfig     # Reddit API settings
│   └── WebClientConfig     # HTTP client configuration
├── controller/             # REST API endpoints
│   ├── RedditController    # Reddit-specific endpoints
│   └── DataProcessingController # Core processing endpoints
├── dto/                    # Data transfer objects
│   ├── SocialPostDto       # Post data transfer
│   ├── AnalyticsReport     # Analytics response
│   └── PostSearchCriteria # Search parameters
├── model/                  # JPA entities
│   ├── SocialPost         # Main post entity
│   ├── SentimentData      # Sentiment analysis results
│   └── reddit/            # Reddit-specific models
├── repository/             # Data access layer
│   ├── SocialPostRepository    # Post queries
│   └── SentimentDataRepository # Sentiment queries
└── service/                # Business logic
    ├── RedditIngestionService  # Reddit data ingestion
    ├── DataProcessingService   # Core processing logic
    ├── RedditApiClient        # Reddit API integration
    └── RateLimiter           # Rate limiting logic
```

## 🔧 Configuration

### Reddit API Settings
```properties
# application.properties
reddit.api.base-url=https://www.reddit.com
reddit.api.user-agent=SentimentAnalytics/1.0 by YourUsername
reddit.api.requests-per-minute=60
reddit.api.max-retries=3
reddit.api.default-subreddits=technology,programming,worldnews
```

### Database Configuration
- **Development**: H2 in-memory database
- **Production**: PostgreSQL (configuration ready)

## 📋 API Reference

### Reddit Endpoints

#### Trigger Manual Ingestion
```http
POST /api/reddit/ingest
Parameters:
  - subreddits: comma-separated list (default: technology,programming)
  - postsPerSubreddit: number of posts per subreddit (default: 25)

Response:
{
  "status": "success",
  "postsIngested": 50,
  "subreddits": ["technology", "programming"]
}
```

#### Get System Statistics
```http
GET /api/reddit/stats

Response:
{
  "status": "healthy",
  "statistics": {
    "totalPosts": 1247,
    "redditPosts": 1247,
    "recentPosts24h": 156,
    "sessionTotal": 50
  }
}
```

#### Health Check
```http
GET /api/reddit/health

Response:
{
  "status": "UP",
  "service": "Reddit Data Ingestion"
}
```

### Data Processing Endpoints

#### Health Check
```http
GET /api/posts/health
```

## 🔍 Data Models

### SocialPost Entity
The core entity representing social media posts across platforms:

```java
// Key fields
String externalId;        // Platform-specific post ID
Platform platform;       // REDDIT or YOUTUBE
String title;            // Post title (both platforms have titles in 2025)
String content;          // Post content
String author;           // Post author
LocalDateTime createdAt; // When post was created
LocalDateTime ingestedAt; // When we ingested it

// Reddit-specific
String subreddit;        // r/technology, r/programming
Long upvotes;           // Reddit upvotes

// YouTube-specific  
String videoId;         // YouTube video ID
Long likeCount;        // YouTube likes
Long viewCount;        // YouTube views

// Common engagement
Long commentCount;      // Comments on the post
Long shareCount;       // Times shared
Double engagementScore; // Calculated engagement metric
```

### Platform Support
```java
public enum Platform {
    REDDIT,    // Supports: subreddit, upvotes, comments
    YOUTUBE    // Supports: videoId, likes, views, comments
}
```

## 🔄 Data Flow

1. **Ingestion**: `RedditController` triggers `RedditIngestionService`
2. **API Call**: `RedditApiClient` fetches data with rate limiting
3. **Processing**: Convert Reddit API response to `SocialPost` entities
4. **Validation**: Check for duplicates and validate data
5. **Storage**: Save to database via `SocialPostRepository`
6. **Analytics**: Ready for sentiment analysis and reporting

## 📈 Performance Features

### Rate Limiting
- Token bucket algorithm
- 60 requests/minute to Reddit API
- Reactive implementation with backpressure

### Duplicate Detection
- Primary: External ID + Platform uniqueness
- Secondary: Content hash comparison
- Configurable similarity threshold (85%)

### Engagement Scoring
- **Reddit**: `upvotes + (comments * 2.5)` with logarithmic scaling
- **YouTube**: Engagement rate based on views + base engagement
- Normalized to 0-10,000 scale

## 🧪 Testing

### Test Data Generation
The system has successfully ingested:
- **288+ Reddit posts** across multiple sessions
- **50-150 posts per API call**
- **100% success rate** for standard subreddit ingestion
- **Sub-100ms response times** for health checks

### Sample Test Commands
```bash
# Test small ingestion
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=programming&postsPerSubreddit=5"

# Test multiple subreddits
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology,programming,MachineLearning&postsPerSubreddit=10"

# Test trending posts
curl -X POST "http://localhost:8080/api/reddit/trending?limit=25"
```

## 🚧 Next Steps

### Immediate Priorities
1. **Analytics Controller**: Expose DataProcessingService analytics via REST APIs
2. **Sentiment Analysis**: Implement NLP processing pipeline
3. **YouTube Integration**: Add YouTube API client similar to Reddit
4. **Advanced Search APIs**: Multi-criteria search with pagination

### Future Enhancements
- Real-time WebSocket notifications
- Distributed caching with Redis
- Advanced sentiment models
- Cross-platform trend correlation
- Production deployment to AWS

## 🔗 Related Documentation

- [Service Architecture](docs/SERVICE_ARCHITECTURE.md) - Detailed service design
- [Data Model](docs/DATA_MODEL.md) - Database schema and relationships
- [API Documentation](docs/API.md) - Complete API reference

Built with ☕ and Spring Boot • [View Project Structure](docs/PROJECT_STRUCTURE.md)