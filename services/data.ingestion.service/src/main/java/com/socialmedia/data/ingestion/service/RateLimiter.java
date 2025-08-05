// src/main/java/com/socialmedia/data/ingestion/service/RateLimiter.java
package com.socialmedia.data.ingestion.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enterprise-grade rate limiter using Token Bucket algorithm
 * Thread-safe and reactive-friendly for high-throughput scenarios
 */
@Component
public class RateLimiter {
    
    private final AtomicInteger tokens;
    private final AtomicReference<Instant> lastRefill;
    private final int maxTokens;
    private final Duration refillInterval;
    private final int refillAmount;
    
    public RateLimiter() {
        // Default: 60 requests per minute (1 per second)
        this.maxTokens = 60;
        this.tokens = new AtomicInteger(maxTokens);
        this.lastRefill = new AtomicReference<>(Instant.now());
        this.refillInterval = Duration.ofSeconds(1);
        this.refillAmount = 1;
    }
    
    public RateLimiter(int requestsPerMinute) {
        this.maxTokens = requestsPerMinute;
        this.tokens = new AtomicInteger(maxTokens);
        this.lastRefill = new AtomicReference<>(Instant.now());
        this.refillInterval = Duration.ofMillis(60000 / requestsPerMinute);
        this.refillAmount = 1;
    }
    
    /**
     * Attempts to acquire a token for rate limiting
     * @return Mono that completes when token is available
     */
    public Mono<Void> acquireToken() {
        return Mono.fromCallable(this::tryAcquireToken)
            .flatMap(acquired -> {
                if (acquired) {
                    return Mono.empty();
                } else {
                    // Wait and retry if no token available
                    Duration waitTime = calculateWaitTime();
                    return Mono.delay(waitTime).then(acquireToken());
                }
            });
    }
    
    /**
     * Non-blocking attempt to acquire a token
     */
    public boolean tryAcquireToken() {
        refillTokens();
        
        int currentTokens = tokens.get();
        if (currentTokens > 0) {
            return tokens.compareAndSet(currentTokens, currentTokens - 1);
        }
        return false;
    }
    
    /**
     * Refill tokens based on elapsed time since last refill
     */
    private void refillTokens() {
        Instant now = Instant.now();
        Instant lastRefillTime = lastRefill.get();
        
        if (Duration.between(lastRefillTime, now).compareTo(refillInterval) >= 0) {
            if (lastRefill.compareAndSet(lastRefillTime, now)) {
                // Calculate how many tokens to add based on elapsed time
                long elapsedIntervals = Duration.between(lastRefillTime, now).toMillis() / refillInterval.toMillis();
                int tokensToAdd = (int) Math.min(elapsedIntervals * refillAmount, maxTokens);
                
                int currentTokens;
                int newTokens;
                do {
                    currentTokens = tokens.get();
                    newTokens = Math.min(currentTokens + tokensToAdd, maxTokens);
                } while (!tokens.compareAndSet(currentTokens, newTokens));
            }
        }
    }
    
    /**
     * Calculate how long to wait before next token becomes available
     */
    private Duration calculateWaitTime() {
        Instant now = Instant.now();
        Instant lastRefillTime = lastRefill.get();
        Duration timeSinceRefill = Duration.between(lastRefillTime, now);
        
        if (timeSinceRefill.compareTo(refillInterval) >= 0) {
            return Duration.ofMillis(100); // Short wait if refill should happen soon
        } else {
            return refillInterval.minus(timeSinceRefill);
        }
    }
    
    /**
     * Get current number of available tokens (for monitoring)
     */
    public int getAvailableTokens() {
        refillTokens();
        return tokens.get();
    }
}