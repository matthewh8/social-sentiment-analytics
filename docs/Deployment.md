# AWS Production Deployment Guide

## Deployment Overview

**Status**: Production Deployment Complete  
**Date**: August 31, 2025  
**Live Application**: http://3.140.252.197:8080/api  
**Architecture**: EC2 + RDS PostgreSQL + ElastiCache Redis

## AWS Infrastructure Components

### 1. EC2 Instance (Application Server)

**Instance Details:**
- **Instance ID**: i-0xxxxx
- **Instance Type**: t2.micro (Free tier eligible)
- **Public IP**: 3.140.252.197
- **AMI**: Amazon Linux 2
- **Region**: us-east-2 (Ohio)
- **Availability Zone**: us-east-2a
- **Key Pair**: socialsentiment-key.pem

**Security Group**: sg-0e5761cc57f39f7c2 (launch-wizard-1)
- **Inbound Rules**:
  - SSH (22): Your IP address only
  - Custom TCP (8080): 0.0.0.0/0 (Public access for Spring Boot)
- **Outbound Rules**: All traffic allowed (default)

**Installed Software:**
```bash
# Java 17 (Amazon Corretto)
java -version
# openjdk version "17.0.7" 2023-04-18 LTS

# Maven 3.9.x
mvn -version

# Application Location
/home/ec2-user/app/services/data.ingestion.service/
```

### 2. RDS PostgreSQL (Database)

**Database Details:**
- **Instance ID**: socialsentiment-db
- **Endpoint**: socialsentiment-db.cx64ikumk10n.us-east-2.rds.amazonaws.com
- **Port**: 5432
- **Engine**: PostgreSQL 16.9
- **Instance Class**: db.t4g.micro (Free tier eligible)
- **Allocated Storage**: 20 GB (gp2)
- **Region**: us-east-2 (Ohio)
- **Multi-AZ**: No (Single AZ for cost)

**Database Configuration:**
- **Database Name**: socialsentiment
- **Master Username**: postgres
- **Master Password**: SocialSentiment2025!
- **Backup Retention**: 7 days
- **Storage Encrypted**: Yes

**Security Group**: socialsentiment-db-sg
- **Inbound Rules**:
  - PostgreSQL (5432): sg-0e5761cc57f39f7c2 (EC2 security group only)
- **No direct internet access** (Private subnet)

### 3. ElastiCache Redis (Caching Layer)

**Cache Details:**
- **Cluster ID**: socialsentiment-redis-cache
- **Configuration Endpoint**: clustercfg.socialsentiment-redis-cache.kobbva.use2.cache.amazonaws.com
- **Port**: 6379
- **Engine**: Redis 7.x (Valkey compatible)
- **Node Type**: cache.t3.micro (Free tier eligible)
- **Region**: us-east-2 (Ohio)

**Cache Configuration:**
- **SSL Encryption**: Enabled
- **Auth Token**: None (VPC security)
- **Backup Retention**: 1 day
- **Automatic Failover**: Disabled (Single node)

**Security Configuration:**
- **VPC Security Group**: Default VPC security group
- **Inbound Rules**: Redis (6379) from EC2 security group
- **No direct internet access** (Private subnet)

## Network Architecture

```
Internet Gateway
       │
       ▼ (Port 8080)
┌─────────────────┐
│   EC2 Instance  │  Public Subnet (10.0.1.0/24)
│  3.140.252.197  │  Spring Boot App
└─────────────────┘
       │
       └─────┬─────────────────────┬─────────
             │                     │
             ▼                     ▼
    ┌─────────────────┐   ┌─────────────────┐
    │ RDS PostgreSQL  │   │ElastiCache Redis│  Private Subnets
    │    (Port 5432)  │   │    (Port 6379)  │  (10.0.2.0/24)
    └─────────────────┘   └─────────────────┘
```

**Key Security Principle**: Only EC2 has internet access. RDS and ElastiCache are private and only accessible from EC2.

## Application Configuration

### Production Configuration File
**Location**: `src/main/resources/application-aws.properties`

```properties
# AWS RDS PostgreSQL Connection
spring.datasource.url=jdbc:postgresql://socialsentiment-db.cx64ikumk10n.us-east-2.rds.amazonaws.com:5432/socialsentiment
spring.datasource.username=postgres
spring.datasource.password=SocialSentiment2025!
spring.datasource.driver-class-name=org.postgresql.Driver

# Connection Pool Settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=false

# AWS ElastiCache Redis Configuration
spring.data.redis.host=clustercfg.socialsentiment-redis-cache.kobbva.use2.cache.amazonaws.com
spring.data.redis.port=6379
spring.data.redis.ssl.enabled=true
spring.data.redis.timeout=5000ms
spring.cache.type=redis

# Redis Connection Pool
spring.data.redis.lettuce.pool.max-active=20
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2

# Application Server
server.port=8080

# Logging
logging.level.com.socialmedia=INFO
logging.level.org.springframework.data.redis=WARN
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

### Environment Variables
```bash
# Set on EC2 instance
export SPRING_PROFILES_ACTIVE=aws
export SERVER_PORT=8080
```

## Deployment Process

### 1. EC2 Instance Setup
```bash
# Connect to EC2
ssh -i ~/.ssh/socialsentiment-key.pem ec2-user@3.140.252.197

# Update system
sudo dnf update -y

# Install Java 17
sudo dnf install java-17-amazon-corretto-devel -y

# Install Maven
sudo dnf install maven -y

# Verify installations
java -version
mvn -version
```

### 2. Application Deployment
```bash
# Clone repository (or upload code)
mkdir -p /home/ec2-user/app
cd /home/ec2-user/app

# Navigate to service directory
cd services/data.ingestion.service

# Set AWS profile
export SPRING_PROFILES_ACTIVE=aws

# Build application (skip tests for faster deployment)
./mvnw clean compile -DskipTests

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=aws
```

### 3. Service Verification
```bash
# Test local connectivity (from EC2)
curl localhost:8080/api/reddit/health

# Expected Response:
{
  "status": "UP",
  "service": "Reddit Data Ingestion",
  "caching": {
    "enabled": true,
    "available": true
  },
  "timestamp": "1692345600000"
}
```

### 4. External Verification
```bash
# Test from any internet location
curl http://3.140.252.197:8080/api/reddit/health

# Performance test
time curl http://3.140.252.197:8080/api/reddit/stats
```

## Connection Testing

### Database Connectivity Test
```bash
# From EC2 instance
nc -zv socialsentiment-db.cx64ikumk10n.us-east-2.rds.amazonaws.com 5432
# Expected: Connection succeeded

# Application database test
curl localhost:8080/api/posts/health
```

### Redis Connectivity Test  
```bash
# From EC2 instance
nc -zv clustercfg.socialsentiment-redis-cache.kobbva.use2.cache.amazonaws.com 6379
# Expected: Connection succeeded

# Application cache test
curl localhost:8080/api/reddit/cache/health
```

## Performance Verification

### Cache Performance Test
```bash
# Cache miss (first call) - should take ~458ms
time curl http://3.140.252.197:8080/api/reddit/stats

# Cache hit (subsequent calls) - should take ~12-14ms
time curl http://3.140.252.197:8080/api/reddit/stats
time curl http://3.140.252.197:8080/api/reddit/stats
```

### Load Test
```bash
# Concurrent requests test
for i in {1..10}; do
  curl -s http://3.140.252.197:8080/api/reddit/stats > /dev/null &
done
wait
echo "All concurrent requests completed"
```

**Expected Results:**
- **Cache Miss**: ~458ms (RDS query)
- **Cache Hit**: ~12-14ms (ElastiCache lookup)
- **Performance Improvement**: 97% faster with cache
- **Concurrent Load**: All 10 requests complete successfully

## Troubleshooting

### Common Connection Issues

**1. Application Not Accessible Externally**
```bash
# Check security group allows port 8080
aws ec2 describe-security-groups --group-ids sg-0e5761cc57f39f7c2

# Verify application is running on correct port
sudo netstat -tlnp | grep 8080
```

**2. Database Connection Failed**
```bash
# Check RDS security group allows EC2 access
aws rds describe-db-instances --db-instance-identifier socialsentiment-db

# Test network connectivity
nc -zv socialsentiment-db.cx64ikumk10n.us-east-2.rds.amazonaws.com 5432
```

**3. Redis Connection Failed**
```bash
# Check ElastiCache cluster status
aws elasticache describe-cache-clusters --cache-cluster-id socialsentiment-redis-cache

# Test network connectivity  
nc -zv clustercfg.socialsentiment-redis-cache.kobbva.use2.cache.amazonaws.com 6379
```

**4. Application Won't Start**
```bash
# Check Java installation
java -version

# Check if port is already in use
sudo lsof -i :8080

# Check application logs
tail -f logs/spring.log
```

### Service Status Commands
```bash
# Check if application is running
ps aux | grep java

# Kill application if needed
pkill -f "spring-boot"

# Check system resources
free -h
df -h
```

## Cost Management

### Free Tier Usage
- **EC2 t2.micro**: 750 hours/month (Always Free)
- **RDS db.t4g.micro**: 750 hours/month (12 months free)
- **ElastiCache cache.t3.micro**: 750 hours/month (12 months free)
- **Data Transfer**: 15 GB outbound/month (Always Free)

### Estimated Monthly Cost
- **Development Phase**: $0-5 (within free tier)
- **After Free Tier**: ~$25-30/month

## Security Configuration

### Access Control
- **SSH Access**: Key-based authentication only
- **Database**: No direct internet access, password protected
- **Cache**: VPC security, no authentication token required
- **Application**: Public HTTP access for demo purposes

### Data Encryption
- **In Transit**: SSL/TLS enabled for RDS and ElastiCache
- **At Rest**: RDS storage encryption enabled
- **Application**: HTTPS recommended for production use

## Backup Configuration

### RDS Automatic Backups
- **Backup Window**: 03:00-04:00 UTC
- **Backup Retention**: 7 days
- **Point-in-Time Recovery**: Enabled
- **Final Snapshot**: Created on deletion

### Manual Backup Commands
```bash
# Create manual RDS snapshot
aws rds create-db-snapshot \
  --db-instance-identifier socialsentiment-db \
  --db-snapshot-identifier socialsentiment-manual-$(date +%Y%m%d)
```

## Production Endpoints

### Live API Endpoints
```bash
# Health checks
curl http://3.140.252.197:8080/api/reddit/health
curl http://3.140.252.197:8080/api/youtube/health
curl http://3.140.252.197:8080/api/posts/health

# Statistics (cached)
curl http://3.140.252.197:8080/api/reddit/stats
curl http://3.140.252.197:8080/api/youtube/stats

# Cache management
curl http://3.140.252.197:8080/api/reddit/cache/health

# Manual ingestion
curl -X POST "http://3.140.252.197:8080/api/reddit/ingest?subreddits=technology&postsPerSubreddit=10"
```

## Monitoring

### Application Health Checks
```bash
# Health endpoint returns AWS service status
curl http://3.140.252.197:8080/api/reddit/health | jq '.caching'
# Expected: {"enabled": true, "available": true}

# Performance monitoring
curl http://3.140.252.197:8080/actuator/metrics/cache.hit.count
curl http://3.140.252.197:8080/actuator/metrics/cache.miss.count
```

### Resource Monitoring
```bash
# From EC2 instance
htop              # CPU and memory usage
iostat -x 1       # Disk I/O
netstat -i        # Network interface statistics
```

## Next Steps

### Immediate Enhancements
1. **SSL Certificate**: Add HTTPS with Let's Encrypt or AWS Certificate Manager
2. **Domain Name**: Configure Route 53 DNS for custom domain
3. **Process Management**: Set up application as systemd service for auto-restart
4. **Log Management**: Configure log rotation and centralized logging

### Scaling Preparation
1. **Application Load Balancer**: Prepare for multiple EC2 instances
2. **Auto Scaling Group**: Configure automatic scaling based on CPU/memory
3. **Multi-AZ RDS**: Enable Multi-AZ for high availability
4. **ElastiCache Cluster**: Configure cluster mode for Redis scaling

## Summary

This AWS deployment demonstrates:
- **Production Infrastructure**: Multi-service AWS architecture with proper security
- **Performance Optimization**: 97% improvement through ElastiCache integration
- **Cost Effectiveness**: Operating within AWS free tier limits
- **Scalability Foundation**: Architecture ready for horizontal scaling
- **Operational Excellence**: Proper monitoring, backup, and troubleshooting procedures

**Live System**: http://3.140.252.197:8080/api - Fully functional and accessible for demonstration and testing.