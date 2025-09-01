# Social Media Sentiment Analytics System

A reactive Spring Boot application for ingesting and analyzing social media data from Reddit and YouTube platforms with unified sentiment analysis capabilities.

## Project Overview

**Current Status**: Dual-platform ingestion system with Reddit and YouTube APIs fully operational  
**Tech Stack**: Java 17, Spring Boot 3.5.4, Spring WebFlux, PostgreSQL, Redis, Docker, Maven  
**Architecture**: Reactive microservices with rate limiting and duplicate detection  

## Features

### Implemented
- **Reddit Integration**: Subreddit-based ingestion with trending posts support
- **YouTube Integration**: Channel-based, search-based, and trending video ingestion  
- **PostgreSQL Database**: Production-ready database with Docker containerization
- **Redis Support**: Ready for caching layer integration
- **Reactive Processing**: Non-blocking I/O with Spring WebFlux Mono/Flux patterns
- **Rate Limiting**: Token bucket algorithm respecting API quotas
- **Duplicate Prevention**: External ID + platform uniqueness checks
- **Engagement Scoring**: Platform-specific algorithms for Reddit and YouTube
- **Statistics Tracking**: Real-time session and historical metrics
- **Health Monitoring**: Service health endpoints for both platforms

### Data Model Ready For
- Sentiment analysis with confidence scoring
- Advanced search with multiple criteria
- Analytics report generation
- Cross-platform trend analysis

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- YouTube Data API v3 key (required for YouTube features)

### Setup

**1. Clone and navigate to project:**
```bash
git clone [your-repo-url]
cd social-media-sentiment-analytics/services/data.ingestion.service
```

**2. Start database services:**
```bash
docker compose up -d
```

**3. Configure YouTube API key:**
```bash
echo "youtube.api.api-key=YOUR_API_KEY_HERE" >> src/main/resources/application.properties
```

**4. Run the application:**
```bash
./mvnw spring-boot:run
```

### Get YouTube API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create project or select existing
3. Enable "YouTube Data API v3"
4. Create credentials (API Key)
5. Add to `application.properties`:
   ```properties
   youtube.api.api-key=YOUR_ACTUAL_API_KEY_HERE
   ```

### Test Basic Functionality
```bash
# Health checks
curl http://localhost:8080/api/reddit/health
curl http://localhost:8080/api/youtube/health

# Reddit ingestion
curl -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology&postsPerSubreddit=10"

# YouTube search ingestion
curl -X POST "http://localhost:8080/api/youtube/ingest/search" \
  -H "Content-Type: application/json" \
  -d '{"query": "programming", "limit": 10}'

# Check results
curl http://localhost:8080/api/reddit/stats
curl http://localhost:8080/api/youtube/stats
```

## API Endpoints

### Reddit API
```http
POST /api/reddit/ingest          # Manual subreddit ingestion
POST /api/reddit/trending        # Trending posts from r/popular  
GET  /api/reddit/stats          # Ingestion statistics
GET  /api/reddit/health         # Health check
GET  /api/reddit/config         # Service configuration
```

### YouTube API  
```http
POST /api/youtube/ingest/channel/{channelId}  # Channel-based ingestion
POST /api/youtube/ingest                      # Multi-channel ingestion
POST /api/youtube/ingest/search               # Search-based ingestion
POST /api/youtube/trending                    # Trending videos
GET  /api/youtube/stats                       # Ingestion statistics
GET  /api/youtube/health                      # Health check
GET  /api/youtube/config                      # Service configuration
```

### Data Processing
```http
GET /api/posts/health           # Data service health check
GET /api/posts/test            # Connectivity test
```

## Database Access

### PostgreSQL (Production Database)
**Connection Details:**
- Host: localhost:5433
- Database: socialsentiment
- Username: postgres
- Password: password123

**Connect via psql:**
```bash
docker exec -it socialsentiment-postgres psql -U postgres -d socialsentiment
```

**Sample Queries:**
```sql
-- View all posts
SELECT * FROM social_posts ORDER BY created_at DESC LIMIT 20;

-- Reddit engagement leaders
SELECT title, author, subreddit, upvotes, engagement_score 
FROM social_posts 
WHERE platform = 'REDDIT' 
ORDER BY engagement_score DESC LIMIT 10;

-- YouTube view leaders
SELECT title, author, view_count, like_count, engagement_score
FROM social_posts 
WHERE platform = 'YOUTUBE' 
ORDER BY view_count DESC LIMIT 10;

-- Cross-platform comparison
SELECT platform, COUNT(*), AVG(engagement_score)
FROM social_posts 
GROUP BY platform;
```

### Redis (Caching Layer)
**Connection Details:**
- Host: localhost:6379
- Ready for caching implementation

**Test Redis:**
```bash
docker exec -it socialsentiment-redis redis-cli ping
```

## Configuration

### Default Settings
```properties
# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5433/socialsentiment
spring.datasource.username=postgres
spring.datasource.password=password123

# Redis Configuration  
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Reddit API  
reddit.api.base-url=https://www.reddit.com
reddit.api.requests-per-minute=60
reddit.api.default-subreddits=technology,programming,worldnews,AskReddit,MachineLearning,artificial

# YouTube API
youtube.api.base-url=https://www.googleapis.com/youtube/v3  
youtube.api.requests-per-second=100
youtube.api.quota-units-per-day=10000
youtube.api.default-channels=UCBJycsmduvYEL83R_U4JriQ,UCXuqSBlHAE6Xw-yeJA0Tunw
```

### Customization
Override any defaults in `application.properties`:
```properties
# Custom subreddits
reddit.api.default-subreddits=MachineLearning,artificial,programming

# Custom YouTube channels
youtube.api.default-channels=YOUR_CHANNEL_ID_1,YOUR_CHANNEL_ID_2

# Rate limiting
reddit.api.requests-per-minute=30
youtube.api.requests-per-second=50
```

## Docker Services

### Current Setup
The application uses Docker Compose for database services:

```yaml
services:
  postgres:
    image: postgres:15
    ports:
      - "5433:5432"  # Avoids conflict with system PostgreSQL
    environment:
      POSTGRES_DB: socialsentiment
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password123

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

### Managing Services
```bash
# Start all services
docker compose up -d

# View logs
docker compose logs postgres
docker compose logs redis

# Stop services
docker compose down

# Reset database (removes all data)
docker compose down -v
docker compose up -d
```

## Development

### Running Tests
```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw test -Dtest="*IntegrationTest"

# Test specific functionality
./mvnw test -Dtest="RedditIngestionServiceTest"
```

### Development Database
The application uses PostgreSQL in Docker containers:
- **Pro**: Production-like environment, persistent data, proper indexing
- **Con**: Requires Docker setup
- **Migration Path**: Ready for AWS RDS deployment

### Logging
View detailed logs during development:
```bash
# In application.properties
logging.level.com.socialmedia=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG
```

## Architecture Decisions

### Why PostgreSQL with Docker
- **Production Ready**: Same database as production deployment
- **Performance**: Proper indexing and constraints
- **Isolation**: Separate from system PostgreSQL on different port
- **Scalability**: Ready for cloud deployment

### Why Port 5433
- **Conflict Avoidance**: System PostgreSQL typically uses 5432
- **Development Safety**: Won't interfere with existing databases
- **Clear Separation**: Docker vs system services

### Why Spring WebFlux
- **Non-blocking I/O**: Handle multiple API calls efficiently
- **Backpressure**: Automatic handling when Reddit/YouTube APIs slow down
- **Reactive Streams**: Natural fit for API data ingestion pipelines

### Why Unified Data Model
- **Cross-platform Analytics**: Compare Reddit vs YouTube engagement
- **Consistent Processing**: Same sentiment analysis for all platforms
- **Scalable Design**: Easy to add new platforms (Twitter, TikTok, etc.)

## Performance Metrics

### Measured Performance
- **Reddit Ingestion**: 25-100 posts per API call
- **YouTube Ingestion**: 25-50 videos per operation  
- **Response Time**: <100ms for health endpoints
- **Database Performance**: Optimized with proper indexes
- **Memory Usage**: Efficient reactive streaming

### Scalability Features
- **Concurrent Processing**: Multiple subreddits/channels processed simultaneously
- **Rate Limit Compliance**: Automatic throttling prevents API blocks
- **Graceful Degradation**: Individual platform failures don't stop other processing
- **Efficient Database**: PostgreSQL with proper indexing and constraints

## Troubleshooting

### Common Issues

**Port Conflicts:**
```bash
# If you see "port 5433 already in use"
docker compose down
lsof -i :5433
# Kill any conflicting processes
```

**YouTube API Key Error:**
```bash
# Error: 403 Forbidden
# Solution: Check API key in application.properties
youtube.api.api-key=YOUR_VALID_API_KEY
```

**Database Connection Issues:**
```bash
# Check if containers are running
docker compose ps

# Check container logs
docker compose logs postgres

# Test direct connection
docker exec -it socialsentiment-postgres psql -U postgres -d socialsentiment
```

**Application Won't Start:**
```bash
# Check if databases are ready
docker compose ps
# Wait for postgres to show "healthy" status

# Check application logs for specific errors
./mvnw spring-boot:run
```

### Debug Mode
```bash
# Run with debug logging
./mvnw spring-boot:run -Dspring.profiles.active=debug

# Check database directly
docker exec -it socialsentiment-postgres psql -U postgres -d socialsentiment
```

## Next Development Phase

This project is designed for a 2-week enhancement sprint to add:

### Week 1: Advanced Features
- Sentiment analysis integration (VADER or Stanford CoreNLP)
- Redis caching implementation
- Advanced search endpoints
- Analytics report generation

### Week 2: Frontend Development
- React dashboard for data visualization
- Real-time updates with polling/WebSocket
- Analytics charts and trend visualization
- Complete full-stack deployment on AWS

## Contributing

### Adding New Platforms
1. Create platform-specific model classes (like `reddit/` package)
2. Add platform enum value
3. Create API client following `RedditApiClient` pattern
4. Create ingestion service following `RedditIngestionService` pattern
5. Add controller endpoints following `RedditController` pattern
6. Update `SocialPost` entity conversion logic

### Code Quality Standards
- Reactive programming with Mono/Flux
- Comprehensive error handling with graceful degradation
- Structured logging with SLF4J
- Jakarta validation for DTOs
- Unit tests for service layer logic

## Documentation

- **API Documentation**: Complete REST endpoint reference in `docs/API.md`
- **Data Model**: Database schema and entity relationships in `docs/DATA_MODEL.md`
- **Service Architecture**: Detailed service layer design in `docs/SERVICE_ARCHITECTURE.md`
- **Configuration**: All available configuration options

## License

[Your License Here]

---

**Status**: Production-ready Reddit and YouTube integration with PostgreSQL database, Redis support configured, ready for sentiment analysis and cloud deployment.