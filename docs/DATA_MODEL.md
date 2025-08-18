# Social Media Analytics - Data Model Documentation

## Overview
MVP focused on core functionality
## Core Entities

### SocialPost
**Purpose**: Store social media posts from multiple platforms

| Field | Type | Required | Platform | Description |
|-------|------|----------|----------|-------------|
| `id` | Long | Yes | All | Primary key |
| `platform` | Platform | Yes | All | REDDIT, TWITTER, YOUTUBE |
| `externalId` | String(100) | Yes | All | Platform's post ID |
| `title` | String(500) | No | Reddit, YouTube | Post/video title (null for Twitter) |
| `content` | Text | Yes | All | Post content/description |
| `author` | String(100) | Yes | All | Username/channel name |
| `createdAt` | DateTime | Yes | All | When post was created |
| `ingestedAt` | DateTime | Yes | All | When we imported it |

**Engagement Metrics** (all nullable, platform-dependent):
| Field | Type | Used By | Description |
|-------|------|---------|-------------|
| `likeCount` | Long | All | Likes/thumbs up |
| `commentCount` | Long | All | Comments/replies |
| `shareCount` | Long | Twitter, YouTube | Retweets/shares |
| `upvotes` | Long | Reddit | Reddit upvotes |
| `viewCount` | Long | YouTube | Video views |
| `engagementScore` | Double | All | Calculated score (0-100) |

**Platform-Specific Context**:
| Field | Type | Platform | Description |
|-------|------|----------|-------------|
| `subreddit` | String(100) | Reddit | Subreddit name |
| `videoId` | String(500) | YouTube | YouTube video ID |

### SentimentData
**Purpose**: Store sentiment analysis results (1:1 with SocialPost)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | Long | Yes | Primary key |
| `socialPost` | SocialPost | Yes | Related post |
| `sentimentLabel` | SentimentLabel | Yes | POSITIVE, NEGATIVE, NEUTRAL |
| `sentimentScore` | Double | Yes | 0.0-1.0 (how positive/negative) |
| `confidence` | Double | Yes | 0.0-1.0 (how confident) |
| `processedAt` | DateTime | Yes | When analyzed |

### Platform (Enum)
- `REDDIT` - Reddit posts
- `TWITTER` - Twitter/X posts  
- `YOUTUBE` - YouTube videos

### SentimentLabel (Enum)
- `POSITIVE` - Positive sentiment
- `NEGATIVE` - Negative sentiment
- `NEUTRAL` - Neutral sentiment

## Database Indexes

### SocialPost Indexes
- `idx_platform_created` - (platform, createdAt) for time-based queries
- `idx_external_platform` - (externalId, platform) for uniqueness
- `idx_engagement` - (engagementScore) for top posts

### SentimentData Indexes
- `idx_sentiment_label` - (sentimentLabel) for filtering
- `idx_sentiment_score` - (sentimentScore) for ranking

## Key Design Decisions

### ✅ What We Kept
- **Universal fields**: Work across all platforms
- **Platform-specific fields**: Only when necessary (subreddit, videoId)
- **Nullable engagement**: Different platforms have different metrics
- **Simple sentiment**: Label + score + confidence is sufficient

### ❌ What We Removed
- **downvotes**: Negative engagement adds complexity
- **hashtags/mentions**: Can extract from content when needed
- **contentHash**: Service layer handles duplicates
- **Complex sentiment**: Individual emotion scores over-engineered
- **Processing metadata**: Algorithm versioning premature

### 🔄 Future Considerations
- **Content features**: Extract hashtags/mentions when building search
- **Advanced sentiment**: Add emotion detection when needed
- **Performance**: Add caching layer before optimizing database
- **New platforms**: TikTok, Instagram would add new platform-specific fields

## Migration Strategy
1. Drop unused columns from existing schema
2. Add `title` field (nullable)
3. Simplify sentiment_data table
4. Remove unnecessary indexes
5. Update service layer to match

## Example Usage

### Reddit Post
```sql
INSERT INTO social_posts (platform, externalId, title, content, author, subreddit, upvotes, commentCount, createdAt)
VALUES ('REDDIT', 'abc123', 'Amazing AI breakthrough!', 'Check out this new model...', 'tech_user', 'MachineLearning', 245, 67, '2025-08-17 10:00:00');
```

### Twitter Post
```sql
INSERT INTO social_posts (platform, externalId, title, content, author, likeCount, shareCount, createdAt)
VALUES ('TWITTER', 'tweet_456', NULL, 'Just shipped a new feature! 🚀', 'startup_founder', 89, 12, '2025-08-17 11:30:00');
```

### YouTube Video
```sql
INSERT INTO social_posts (platform, externalId, title, content, author, videoId, viewCount, likeCount, createdAt)
VALUES ('YOUTUBE', 'vid_789', '10 Minute Python Tutorial', 'Learn Python basics in just 10 minutes...', 'CodeAcademy', 'dQw4w9WgXcQ', 15432, 892, '2025-08-17 09:15:00');
```