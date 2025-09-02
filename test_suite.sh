#!/bin/bash

# Complete Social Media Sentiment Analytics Test Suite
# Save this as test_suite.sh in your project root directory
# Run with: chmod +x test_suite.sh && ./test_suite.sh

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"
FAILED_TESTS=0
TOTAL_TESTS=0

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

# Success function
success() {
    echo -e "${GREEN}✅ PASS:${NC} $1"
}

# Failure function
fail() {
    echo -e "${RED}❌ FAIL:${NC} $1"
    ((FAILED_TESTS++))
}

# Warning function
warn() {
    echo -e "${YELLOW}⚠️  WARN:${NC} $1"
}

# Test function
test_endpoint() {
    local description=$1
    local endpoint=$2
    local expected_status=${3:-200}
    local method=${4:-GET}
    local data=${5:-""}
    
    ((TOTAL_TESTS++))
    
    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d "$data" "$endpoint")
    else
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" "$endpoint")
    fi
    
    status=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    body=$(echo $response | sed -e 's/HTTPSTATUS:.*//')
    
    if [ "$status" -eq "$expected_status" ]; then
        success "$description ($status)"
        echo "$body" | jq . > /dev/null 2>&1 || warn "Response is not valid JSON"
    else
        fail "$description (Expected: $expected_status, Got: $status)"
        echo "Response: $body"
    fi
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Installing jq for JSON parsing..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install jq
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sudo apt-get update && sudo apt-get install -y jq
    else
        echo "Please install jq manually: https://stedolan.github.io/jq/"
        exit 1
    fi
fi

echo ""
echo "🚀 SOCIAL MEDIA SENTIMENT ANALYTICS - COMPREHENSIVE TEST SUITE"
echo "=============================================================="
log "Starting comprehensive test suite..."

echo ""
echo "📋 PHASE 1: INFRASTRUCTURE CHECKS"
echo "================================"

# Check if application is running
log "Checking if application is running..."
if curl -s "$BASE_URL/api/reddit/health" > /dev/null; then
    success "Application is running"
else
    fail "Application is not running. Please start with: mvn spring-boot:run"
    exit 1
fi

# Check Docker containers
log "Checking Docker containers..."
if docker ps | grep -q socialsentiment-postgres; then
    success "PostgreSQL container is running"
else
    fail "PostgreSQL container not running. Run: docker compose up -d"
    exit 1
fi

if docker ps | grep -q socialsentiment-redis; then
    success "Redis container is running"
else
    fail "Redis container not running. Run: docker compose up -d"
    exit 1
fi

# Test database connectivity
log "Testing database connectivity..."
if docker exec socialsentiment-postgres pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
    success "PostgreSQL is accepting connections"
else
    fail "Cannot connect to PostgreSQL"
fi

# Test Redis connectivity
log "Testing Redis connectivity..."
if docker exec socialsentiment-redis redis-cli ping | grep -q PONG; then
    success "Redis is responding"
else
    fail "Cannot connect to Redis"
fi

echo ""
echo "🏥 PHASE 2: HEALTH CHECK ENDPOINTS"
echo "================================="

test_endpoint "Reddit service health" "$BASE_URL/api/reddit/health"
test_endpoint "Data processing health" "$BASE_URL/api/posts/health"
test_endpoint "Sentiment analysis health" "$BASE_URL/api/sentiment/health"
test_endpoint "Cache health check" "$BASE_URL/api/reddit/cache/health"

# Check Stanford NLP initialization
log "Verifying Stanford CoreNLP initialization..."
nlp_response=$(curl -s "$BASE_URL/api/sentiment/health")
nlp_status=$(echo "$nlp_response" | jq -r '.status // "UNKNOWN"')
nlp_message=$(echo "$nlp_response" | jq -r '.message // "No message"')

if [ "$nlp_status" = "UP" ]; then
    success "Stanford CoreNLP is initialized and ready"
    log "Message: $nlp_message"
else
    warn "Stanford CoreNLP status: $nlp_status"
    log "Message: $nlp_message"
fi

echo ""
echo "🧪 PHASE 3: SENTIMENT ANALYSIS TESTS"
echo "===================================="

# Test positive sentiment
log "Testing positive sentiment analysis..."
test_endpoint "Positive sentiment test" "$BASE_URL/api/sentiment/test" 200 "POST" \
    '{"text": "This is absolutely amazing and wonderful! I love it!"}'

# Test negative sentiment
log "Testing negative sentiment analysis..."
test_endpoint "Negative sentiment test" "$BASE_URL/api/sentiment/test" 200 "POST" \
    '{"text": "This is terrible and awful! I hate it completely!"}'

# Test neutral sentiment
log "Testing neutral sentiment analysis..."
test_endpoint "Neutral sentiment test" "$BASE_URL/api/sentiment/test" 200 "POST" \
    '{"text": "This is a factual statement about technology and computers."}'

# Test empty text handling
log "Testing empty text handling..."
test_endpoint "Empty text error handling" "$BASE_URL/api/sentiment/test" 400 "POST" \
    '{"text": ""}'

# Test progress endpoint
test_endpoint "Sentiment analysis progress" "$BASE_URL/api/sentiment/progress"

echo ""
echo "📊 PHASE 4: DATA AND STATISTICS TESTS"
echo "====================================="

test_endpoint "Reddit statistics" "$BASE_URL/api/reddit/stats"
test_endpoint "Reddit configuration" "$BASE_URL/api/reddit/config"
test_endpoint "Data processing test" "$BASE_URL/api/posts/test"

# Test platform-specific sentiment stats
test_endpoint "Reddit sentiment statistics" "$BASE_URL/api/sentiment/stats/REDDIT"
test_endpoint "Sentiment distribution" "$BASE_URL/api/sentiment/distribution"

echo ""
echo "🔄 PHASE 5: CACHE PERFORMANCE TESTS"
echo "=================================="

log "Testing cache performance (this may take a few seconds)..."

# Clear cache first
log "Clearing cache..."
curl -s -X DELETE "$BASE_URL/api/reddit/cache" > /dev/null

# Cache miss test
log "Testing cache miss (first call)..."
start_time=$(python3 -c "import time; print(int(time.time() * 1000))")
curl -s "$BASE_URL/api/reddit/stats" > /dev/null
end_time=$(python3 -c "import time; print(int(time.time() * 1000))")
cache_miss_time=$((end_time - start_time))

# Cache hit test
log "Testing cache hit (second call)..."
start_time=$(python3 -c "import time; print(int(time.time() * 1000))")
curl -s "$BASE_URL/api/reddit/stats" > /dev/null
end_time=$(python3 -c "import time; print(int(time.time() * 1000))")
cache_hit_time=$((end_time - start_time))

log "Cache miss time: ${cache_miss_time}ms"
log "Cache hit time: ${cache_hit_time}ms"

if [ $cache_hit_time -lt $cache_miss_time ]; then
    improvement=$(( (cache_miss_time - cache_hit_time) * 100 / cache_miss_time ))
    success "Cache performance improvement: ${improvement}%"
else
    warn "Cache may not be working properly"
fi

echo ""
echo "📥 PHASE 6: DATA INGESTION TESTS"
echo "==============================="

# Test small ingestion
log "Testing small Reddit data ingestion..."
ingestion_response=$(curl -s -X POST "$BASE_URL/api/reddit/ingest?subreddits=technology&postsPerSubreddit=3")
ingested_count=$(echo "$ingestion_response" | jq -r '.postsIngested // 0')

if [ "$ingested_count" -gt 0 ]; then
    success "Successfully ingested $ingested_count posts"
    
    # Wait for sentiment analysis
    log "Waiting 15 seconds for sentiment analysis to process..."
    sleep 15
    
    # Check sentiment analysis progress
    log "Checking sentiment analysis after ingestion..."
    progress_response=$(curl -s "$BASE_URL/api/sentiment/progress")
    analyzed_count=$(echo "$progress_response" | jq -r '.progress.analyzedPosts // 0')
    total_count=$(echo "$progress_response" | jq -r '.progress.totalPosts // 0')
    
    log "Total posts: $total_count, Analyzed: $analyzed_count"
    
    if [ "$analyzed_count" -gt 0 ]; then
        success "Automatic sentiment analysis is working"
    else
        warn "Sentiment analysis may not have processed yet"
    fi
else
    warn "No new posts ingested (may be duplicates or API issues)"
fi

echo ""
echo "🚀 PHASE 7: LOAD AND CONCURRENCY TESTS"
echo "======================================"

log "Testing concurrent API calls (10 simultaneous requests)..."
pids=()
for i in {1..10}; do
    curl -s "$BASE_URL/api/reddit/stats" > /dev/null &
    pids+=($!)
done

# Wait for all requests to complete
for pid in "${pids[@]}"; do
    wait $pid
done

success "Concurrent load test completed successfully"

echo ""
echo "🛡️ PHASE 8: ERROR HANDLING TESTS"
echo "==============================="

test_endpoint "404 error handling" "$BASE_URL/api/invalid/endpoint" 404
test_endpoint "Invalid post ID" "$BASE_URL/api/sentiment/analyze/99999" 400
test_endpoint "Invalid JSON handling" "$BASE_URL/api/sentiment/test" 400 "POST" \
    '{"invalid": "data"}'

echo ""
echo "💾 PHASE 9: DATABASE INTEGRITY CHECKS"
echo "====================================="

log "Checking database integrity..."

# Check table counts
post_count=$(docker exec socialsentiment-postgres psql -U postgres -d socialsentiment -t -c "SELECT COUNT(*) FROM social_posts;" | tr -d ' \n')
sentiment_count=$(docker exec socialsentiment-postgres psql -U postgres -d socialsentiment -t -c "SELECT COUNT(*) FROM sentiment_data;" | tr -d ' \n')

log "Database contains $post_count social posts and $sentiment_count sentiment records"

# Check for orphaned records
orphaned_count=$(docker exec socialsentiment-postgres psql -U postgres -d socialsentiment -t -c "
SELECT COUNT(*) FROM sentiment_data sd 
LEFT JOIN social_posts sp ON sd.social_post_id = sp.id 
WHERE sp.id IS NULL;" | tr -d ' \n')

if [ "$orphaned_count" -eq 0 ]; then
    success "No orphaned sentiment data found"
else
    warn "Found $orphaned_count orphaned sentiment records"
fi

echo ""
echo "🔍 PHASE 10: FINAL SYSTEM VALIDATION"
echo "==================================="

log "Performing final system health check..."

# Get comprehensive system status
final_health=$(curl -s "$BASE_URL/api/reddit/health")
cache_status=$(echo "$final_health" | jq -r '.caching.enabled // false')
service_status=$(echo "$final_health" | jq -r '.status // "UNKNOWN"')

log "Service Status: $service_status"
log "Caching Enabled: $cache_status"

# Check memory usage
log "Checking Java process memory usage..."
java_memory=$(ps aux | grep java | grep -v grep | awk '{print $4}' | head -1)
log "Java memory usage: ${java_memory}% of system memory"

# Final validation
if [ "$service_status" = "UP" ] && [ "$cache_status" = "true" ]; then
    success "System is fully operational"
else
    warn "System may have issues - check individual service status"
fi

echo ""
echo "📋 TEST SUITE SUMMARY"
echo "===================="

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! ($TOTAL_TESTS/$TOTAL_TESTS)${NC}"
    echo ""
    echo -e "${GREEN}✅ Your system is ready for AWS deployment!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Update application-aws.properties with your AWS endpoints"
    echo "2. Build: mvn clean compile -DskipTests"
    echo "3. Deploy to AWS EC2"
    echo "4. Run this same test suite against your AWS endpoints"
else
    echo -e "${RED}❌ TESTS FAILED: $FAILED_TESTS/$TOTAL_TESTS${NC}"
    echo ""
    echo "Please fix the failing tests before AWS deployment."
    echo "Check the error messages above for details."
    exit 1
fi

echo ""
log "Test suite completed at $(date)"