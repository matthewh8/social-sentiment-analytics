#!/bin/bash
set -e

# Configuration
EC2_HOST="3.140.252.197"
EC2_USER="ec2-user"
KEY_PATH="~/.ssh/socialsentiment-key.pem"

echo "Deploying Social Media Analytics with Stanford CoreNLP to EC2..."

ssh -i $KEY_PATH $EC2_USER@$EC2_HOST << 'ENDSSH'
    set -e
    
    echo "=== Updating application from Git ==="
    cd /home/ec2-user/app
    git pull origin main
    
    echo "=== Stopping existing application ==="
    pkill -f "spring-boot" || true
    sleep 5
    
    echo "=== Building application with Stanford CoreNLP ==="
    cd services/data.ingestion.service
    
    # Clean build with Stanford CoreNLP dependencies (this will download ~500MB of models)
    echo "Note: First build will download Stanford CoreNLP models (~500MB)"
    ./mvnw clean compile -DskipTests -q
    
    echo "=== Setting up environment ==="
    export SPRING_PROFILES_ACTIVE=aws
    export JVM_OPTS="-Xmx3g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    
    echo "=== Starting application with Stanford CoreNLP ==="
    # Increased memory for Stanford CoreNLP models and sentiment processing
    nohup ./mvnw spring-boot:run \
        -Dspring-boot.run.profiles=aws \
        -Dspring-boot.run.jvmArguments="$JVM_OPTS" \
        > /home/ec2-user/app.log 2>&1 &
    
    echo "=== Waiting for application startup ==="
    sleep 30  # Stanford CoreNLP initialization takes longer (~15-20 seconds)
    
    echo "=== Health checks ==="
    # Check main application
    if curl -s http://localhost:8080/api/reddit/health > /dev/null; then
        echo "✓ Main application health check passed"
    else
        echo "✗ Main application health check failed"
        exit 1
    fi
    
    # Check Stanford CoreNLP sentiment service
    if curl -s http://localhost:8080/api/sentiment/health > /dev/null; then
        echo "✓ Stanford sentiment service health check passed"
    else
        echo "⚠ Stanford sentiment service not ready yet (may still be initializing)"
        echo "Check logs: tail -f /home/ec2-user/app.log"
    fi
    
    echo "=== Testing sentiment analysis ==="
    # Test sentiment analysis with a simple POST request
    curl -X POST http://localhost:8080/api/sentiment/test \
        -H "Content-Type: application/json" \
        -d '{"text":"This is a great improvement to our system!"}' \
        > /dev/null && echo "✓ Sentiment analysis test passed" || echo "⚠ Sentiment test pending (service may still be initializing)"
    
    echo "=== Deployment Status ==="
    echo "Application: http://3.140.252.197:8080/api"
    echo "Sentiment API: http://3.140.252.197:8080/api/sentiment"
    echo "Logs: tail -f /home/ec2-user/app.log"
    echo ""
    echo "Available Endpoints:"
    echo "- GET  /api/sentiment/health         # Service health check"
    echo "- POST /api/sentiment/test           # Test sentiment analysis"
    echo "- POST /api/sentiment/analyze/{id}   # Analyze specific post"
    echo "- POST /api/sentiment/analyze/batch  # Batch analysis"
    echo "- GET  /api/sentiment/stats/reddit   # Reddit sentiment stats"
    echo "- GET  /api/sentiment/stats/youtube  # YouTube sentiment stats"
    
ENDSSH

echo "=== External Health Checks ==="
echo "Testing external connectivity..."

# Wait for application to fully initialize
sleep 10

# Test main API
if curl -s http://3.140.252.197:8080/api/reddit/health > /dev/null; then
    echo "✓ External API connectivity confirmed"
else
    echo "⚠ External API not yet available"
fi

# Test sentiment API
if curl -s http://3.140.252.197:8080/api/sentiment/health > /dev/null; then
    echo "✓ External sentiment API connectivity confirmed"
else
    echo "⚠ External sentiment API not yet available (Stanford CoreNLP may still be loading)"
fi

echo ""
echo "=== Deployment Complete ==="
echo "🚀 Live Application: http://3.140.252.197:8080/api"
echo "🧠 Sentiment Analysis: http://3.140.252.197:8080/api/sentiment"
echo ""
echo "Next Steps:"
echo "1. Test sentiment analysis: curl -X POST http://3.140.252.197:8080/api/sentiment/test -H 'Content-Type: application/json' -d '{\"text\":\"I love this new feature!\"}'"
echo "2. Trigger batch analysis: curl -X POST http://3.140.252.197:8080/api/sentiment/analyze/batch"
echo "3. Check sentiment stats: curl http://3.140.252.197:8080/api/sentiment/stats/reddit"
echo ""
echo "If sentiment service is still initializing, check logs:"
echo "ssh -i ~/.ssh/socialsentiment-key.pem ec2-user@3.140.252.197 'tail -f /home/ec2-user/app.log'"