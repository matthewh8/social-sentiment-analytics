#!/bin/bash

# Reddit Service Test Commands
# Make sure your Spring Boot application is running on localhost:8080

echo "==========================================="
echo "REDDIT SERVICE COMPREHENSIVE TEST SUITE"
echo "==========================================="

BASE_URL="http://localhost:8080/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to test endpoint and show results
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local expected_status=$4
    local data=$5
    
    echo -e "\n${YELLOW}Testing: ${description}${NC}"
    echo "Endpoint: ${method} ${endpoint}"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "${endpoint}")
    elif [ "$method" = "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "${endpoint}")
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "${endpoint}")
    fi
    
    http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS:/d')
    
    if [ "$http_status" = "$expected_status" ]; then
        echo -e "${GREEN}✓ SUCCESS (${http_status})${NC}"
    else
        echo -e "${RED}✗ FAILED - Expected: ${expected_status}, Got: ${http_status}${NC}"
    fi
    
    echo "Response:"
    echo "$body" | jq . 2>/dev/null || echo "$body"
    echo "---"
}

# 1. HEALTH CHECKS
echo -e "\n${YELLOW}=== HEALTH CHECKS ===${NC}"

test_endpoint "GET" "${BASE_URL}/reddit/health" \
    "Reddit Service Health Check" "200"

test_endpoint "GET" "${BASE_URL}/posts/health" \
    "Data Processing Service Health Check" "200"

test_endpoint "GET" "${BASE_URL}/posts/test" \
    "Data Processing Test Endpoint" "200"

# 2. BASIC STATISTICS
echo -e "\n${YELLOW}=== SYSTEM STATISTICS ===${NC}"

test_endpoint "GET" "${BASE_URL}/reddit/stats" \
    "System Statistics" "200"

test_endpoint "GET" "${BASE_URL}/reddit/config" \
    "Service Configuration" "200"

# 3. MANUAL INGESTION TESTS
echo -e "\n${YELLOW}=== MANUAL INGESTION TESTS ===${NC}"

# Basic ingestion with default parameters
test_endpoint "POST" "${BASE_URL}/reddit/ingest" \
    "Basic Manual Ingestion (default params)" "200"

# Wait a moment between requests to respect rate limiting
echo "Waiting 3 seconds for rate limiting..."
sleep 3

# Custom subreddits with specific post count
test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=programming,technology&postsPerSubreddit=10" \
    "Custom Subreddits with Post Limit" "200"

sleep 3

# Single subreddit test
test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=AskReddit&postsPerSubreddit=5" \
    "Single Subreddit Ingestion" "200"

sleep 3

# Machine Learning subreddit
test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=MachineLearning&postsPerSubreddit=15" \
    "MachineLearning Subreddit" "200"

sleep 3

# 4. TRENDING POSTS
echo -e "\n${YELLOW}=== TRENDING POSTS INGESTION ===${NC}"

test_endpoint "POST" "${BASE_URL}/reddit/trending?limit=20" \
    "Trending Posts from r/popular" "200"

sleep 3

# 5. EDGE CASES AND ERROR HANDLING
echo -e "\n${YELLOW}=== EDGE CASES & ERROR HANDLING ===${NC}"

# Invalid subreddit name
test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=thisisnotarealsubredditname123&postsPerSubreddit=5" \
    "Invalid Subreddit Name" "200"

sleep 2

# Zero posts per subreddit
test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=programming&postsPerSubreddit=0" \
    "Zero Posts Per Subreddit" "200"

sleep 2

# Very high post count (should be limited by Reddit API)
test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=programming&postsPerSubreddit=1000" \
    "High Post Count Request" "200"

sleep 3

# 6. STRESS TEST (Multiple Rapid Requests)
echo -e "\n${YELLOW}=== STRESS TEST (Rate Limiting) ===${NC}"

echo "Sending 5 rapid requests to test rate limiting..."
for i in {1..5}; do
    echo "Request $i:"
    test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=programming&postsPerSubreddit=3" \
        "Rapid Request $i" "200"
done

# 7. POST-INGESTION VERIFICATION
echo -e "\n${YELLOW}=== POST-INGESTION VERIFICATION ===${NC}"

echo "Waiting 5 seconds for ingestion to complete..."
sleep 5

# Check final statistics
test_endpoint "GET" "${BASE_URL}/reddit/stats" \
    "Final Statistics After All Tests" "200"

# 8. LOAD TEST SIMULATION
echo -e "\n${YELLOW}=== LOAD TEST SIMULATION ===${NC}"

echo "Running concurrent ingestion requests..."
for i in {1..3}; do
    (test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=technology&postsPerSubreddit=5" \
        "Concurrent Request $i" "200" &)
done

wait # Wait for all background jobs to complete

echo -e "\n${GREEN}=== ALL TESTS COMPLETED ===${NC}"

# 9. CUSTOM TEST FUNCTIONS
echo -e "\n${YELLOW}=== CUSTOM TEST FUNCTIONS ===${NC}"

# Function to test specific subreddit
test_subreddit() {
    local subreddit=$1
    local limit=${2:-10}
    echo -e "\n${YELLOW}Testing subreddit: r/${subreddit}${NC}"
    test_endpoint "POST" "${BASE_URL}/reddit/ingest?subreddits=${subreddit}&postsPerSubreddit=${limit}" \
        "Subreddit r/${subreddit}" "200"
}

# Function to monitor ingestion progress
monitor_stats() {
    echo -e "\n${YELLOW}Monitoring system statistics for 30 seconds...${NC}"
    for i in {1..6}; do
        echo "Check $i:"
        curl -s "${BASE_URL}/reddit/stats" | jq '.statistics' 2>/dev/null || echo "Stats unavailable"
        if [ $i -lt 6 ]; then
            sleep 5
        fi
    done
}

# Example usage of custom functions
echo "Running custom test examples..."
test_subreddit "Python" 8
sleep 3
test_subreddit "webdev" 5
sleep 2

# Monitor final stats
monitor_stats

echo -e "\n${GREEN}✓ REDDIT SERVICE TEST SUITE COMPLETED${NC}"
echo "Check the application logs for detailed ingestion information."

# 10. PERFORMANCE BENCHMARK
echo -e "\n${YELLOW}=== PERFORMANCE BENCHMARK ===${NC}"

benchmark_ingestion() {
    local subreddit=$1
    local posts=$2
    
    echo "Benchmarking ingestion of ${posts} posts from r/${subreddit}..."
    start_time=$(date +%s.%N)
    
    response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST \
        "${BASE_URL}/reddit/ingest?subreddits=${subreddit}&postsPerSubreddit=${posts}")
    
    end_time=$(date +%s.%N)
    duration=$(echo "$end_time - $start_time" | bc)
    
    http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS:/d')
    
    echo "Duration: ${duration} seconds"
    echo "Status: ${http_status}"
    echo "Response: $(echo "$body" | jq -c . 2>/dev/null || echo "$body")"
}

benchmark_ingestion "programming" 25
sleep 3
benchmark_ingestion "technology" 20
