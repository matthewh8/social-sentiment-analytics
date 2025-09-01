#!/bin/bash
set -e

# Configuration
EC2_HOST="3.140.252.197"
EC2_USER="ec2-user"
KEY_PATH="~/.ssh/socialsentiment-key.pem"

echo "Deploying to EC2..."

ssh -i $KEY_PATH $EC2_USER@$EC2_HOST << 'ENDSSH'
    set -e
    cd /home/ec2-user/app
    git pull origin main
    cd services/data.ingestion.service
    pkill -f "spring-boot" || true
    ./mvnw clean compile -DskipTests -q
    export SPRING_PROFILES_ACTIVE=aws
    nohup ./mvnw spring-boot:run -Dspring-boot.run.profiles=aws > /dev/null 2>&1 &
    sleep 10
    curl -s http://localhost:8080/api/reddit/health > /dev/null && echo "Deployment successful" || echo "Health check failed"
ENDSSH
