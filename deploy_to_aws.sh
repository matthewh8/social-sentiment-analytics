#!/bin/bash
set -e

# Configuration
EC2_HOST="3.140.252.197"
EC2_USER="ec2-user"
KEY_PATH="~/.ssh/socialsentiment-key.pem"
PROJECT_DIR="social-media-sentiment-analytics"

echo "Starting AWS deployment with sentiment analysis..."

# Deploy and run comprehensive tests
ssh -i $KEY_PATH $EC2_USER@$EC2_HOST << 'ENDSSH'
    set -e
    
    echo "=== AWS DEPLOYMENT PHASE ==="
    
    # Navigate to project or clone if needed
    if [ ! -d "/home/ec2-user/app" ]; then
        echo "Cloning repository..."
        mkdir -p /home/ec2-user/app
        cd /home/ec2-user/app
        git clone https://github.com/matthewh8/social-media-sentiment-analytics.git .
    else
        echo "Updating repository..."
        cd /home/ec2-user/app
        git pull origin main
    fi
    
    cd services/data.ingestion.service
    
    # Stop existing application
    echo "Stopping existing application..."
    pkill -f "spring-boot" || true
    sleep 5
    
    # Build application
    echo "Building application..."
    ./mvnw clean compile -DskipTests -q
    
    # Start application with AWS profile
    echo "Starting application with AWS profile..."
    export SPRING_PROFILES_ACTIVE=aws
    export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC -Djna.nosys=true"
    
    nohup ./mvnw spring-boot:run \
        -Dspring-boot.run.profiles=aws \
        -Dspring-boot.run.jvmArguments="-Xmx4g -XX:+UseG1GC -Djna.nosys=true" \
        > /home/ec2-user/app.log 2>&1 &
    
    echo "Waiting for application to start (60 seconds)..."
    sleep 60
    
    echo "=== AWS HEALTH CHECKS ==="
    
    # Basic health check
    if curl -s http://localhost:8080/api/reddit/health > /dev/null; then
        echo "✅ Application is running"
    else
        echo "❌ Health check failed"
        tail -50 /home/ec2-user/app.log
        exit 1
    fi
    
    # Test all critical endpoints
    echo "Testing critical endpoints..."
    
    # Reddit health
    reddit_status=$(curl -s http://localhost:8080/api/reddit/health | jq -r '.status // "UNKNOWN"')
    echo "Reddit service: $reddit_status"
    
    # Sentiment health
    sentiment_status=$(curl -s http://localhost:8080/api/sentiment/health | jq -r '.status // "UNKNOWN"')
    echo "Sentiment service: $sentiment_status"
    
    # Cache health
    cache_status=$(curl -s http://localhost:8080/api/reddit/cache/health | jq -r '.cacheHealth.connected // false')
    echo "AWS ElastiCache: $cache_status"
    
    if [ "$sentiment_status" = "UP" ]; then
        echo "✅ Stanford CoreNLP initialized successfully on AWS"
    else
        echo "❌ Stanford CoreNLP failed to initialize"
        tail -100 /home/ec2-user/app.log | grep -i "stanford\|nlp\|sentiment"
        exit 1
    fi
    
    echo "=== AWS FUNCTIONALITY TESTS ==="
    
    # Test sentiment analysis
    echo "Testing sentiment analysis..."
    sentiment_test=$(curl -s -X POST "http://localhost:8080/api/sentiment/test" \
        -H "Content-Type: application/json" \
        -d '{"text": "This AWS deployment is working great!"}')
    
    sentiment_label=$(echo "$sentiment_test" | jq -r '.result.label // "FAILED"')
    echo "Sentiment test result: $sentiment_label"
    
    if [ "$sentiment_label" != "FAILED" ] && [ "$sentiment_label" != "null" ]; then
        echo "✅ Sentiment analysis working on AWS"
    else
        echo "❌ Sentiment analysis failed on AWS"
        echo "Response: $sentiment_test"
    fi
    
    # Test cache performance
    echo "Testing AWS ElastiCache performance..."
    echo "Cache miss (first call):"
    time curl -s http://localhost:8080/api/reddit/stats > /dev/null
    echo "Cache hit (second call):"
    time curl -s http://localhost:8080/api/reddit/stats > /dev/null
    
    # Test ingestion with sentiment
    echo "Testing data ingestion with sentiment analysis..."
    ingestion_result=$(curl -s -X POST "http://localhost:8080/api/reddit/ingest?subreddits=technology&postsPerSubreddit=2")
    ingested_count=$(echo "$ingestion_result" | jq -r '.postsIngested // 0')
    echo "Ingested $ingested_count posts"
    
    if [ "$ingested_count" -gt 0 ]; then
        echo "Waiting 30 seconds for sentiment analysis..."
        sleep 30
        
        # Check progress
        progress=$(curl -s http://localhost:8080/api/sentiment/progress)
        total_posts=$(echo "$progress" | jq -r '.progress.totalPosts // 0')
        analyzed_posts=$(echo "$progress" | jq -r '.progress.analyzedPosts // 0')
        echo "Total posts: $total_posts, Analyzed: $analyzed_posts"
        
        if [ "$analyzed_posts" -gt 0 ]; then
            echo "✅ Automatic sentiment analysis working on AWS"
        else
            echo "⚠️  Sentiment analysis may still be processing"
        fi
    fi
    
    echo "=== DEPLOYMENT SUMMARY ==="
    echo "Application URL: http://$EC2_HOST:8080"
    echo "Health Check: http://$EC2_HOST:8080/api/reddit/health"
    echo "Sentiment Test: http://$EC2_HOST:8080/api/sentiment/health"
    echo "Log Location: /home/ec2-user/app.log"
    
    # Final comprehensive status
    echo ""
    echo "Final system status:"
    curl -s http://localhost:8080/api/reddit/health | jq '{
        service: .service,
        status: .status,
        caching: .caching.enabled,
        timestamp: .timestamp
    }'
    
    curl -s http://localhost:8080/api/sentiment/health | jq '{
        service: .service,
        status: .status,
        message: .message
    }'
    
    echo "✅ AWS deployment completed successfully!"
ENDSSH

echo ""
echo "=== EXTERNAL TESTING ==="
echo "Testing from external network..."

# External health checks
echo "Testing external connectivity..."
if curl -s http://$EC2_HOST:8080/api/reddit/health > /dev/null; then
    echo "✅ External access working"
    
    # Test sentiment analysis from external network
    echo "Testing external sentiment analysis..."
    external_test=$(curl -s -X POST "http://$EC2_HOST:8080/api/sentiment/test" \
        -H "Content-Type: application/json" \
        -d '{"text": "Testing from external network!"}')
    
    external_label=$(echo "$external_test" | jq -r '.result.label // "FAILED"')
    echo "External sentiment test: $external_label"
    
    # Test cache performance from internet
    echo "Testing cache performance from external network:"
    time curl -s http://$EC2_HOST:8080/api/reddit/stats > /dev/null
    time curl -s http://$EC2_HOST:8080/api/reddit/stats > /dev/null
    
else
    echo "❌ External access failed"
    echo "Check EC2 security group allows port 8080"
fi

echo ""
echo "🚀 DEPLOYMENT COMPLETE!"
echo "Your sentiment analysis system is live at: http://$EC2_HOST:8080"
echo ""
echo "Quick verification commands:"
echo "curl http://$EC2_HOST:8080/api/sentiment/health"
echo "curl http://$EC2_HOST:8080/api/reddit/stats"
echo ""
echo "To monitor logs: ssh -i $KEY_PATH $EC2_USER@$EC2_HOST 'tail -f /home/ec2-user/app.log'"