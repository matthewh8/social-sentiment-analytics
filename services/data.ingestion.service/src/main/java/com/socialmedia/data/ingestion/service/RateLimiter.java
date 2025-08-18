package com.socialmedia.data.ingestion.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simplified rate limiter for MVP
 * Removed: complex algorithms, enterprise features, performance monitoring
 * Kept: basic token bucket, simple reactive support
 */
@Component
public class RateLimiter {
    
    private final AtomicInteger tokens;
    private final AtomicReference<Instant> lastRefill;
    private final int maxTokens;
    private final Duration refillInterval;
    
    public RateLimiter() {
        // Default: 60 requests per minute
        this.maxTokens = 60;
        this.tokens = new AtomicInteger(maxTokens);
        this.lastRefill = new AtomicReference<>(Instant.now());
        this.refillInterval = Duration.ofSeconds(1); // Refill 1 token per second
    }
    
    public RateLimiter(int requestsPerMinute) {
        this.maxTokens = requestsPerMinute;
        this.tokens = new AtomicInteger(maxTokens);
        this.lastRefill = new AtomicReference<>(Instant.now());
        this.refillInterval = Duration.ofMillis(60000 / requestsPerMinute);
    }
    
    /**
     * Reactive token acquisition
     */
    public Mono<Void> acquireToken() {
        return Mono.fromCallable(this::tryAcquireToken)
            .flatMap(acquired -> {
                if (acquired) {
                    return Mono.empty();
                } else {
                    // Simple wait and retry
                    return Mono.delay(Duration.ofMillis(1000)).then(acquireToken());
                }
            });
    }
    
    /**
     * Try to acquire a token (non-blocking)
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
     * Simple token refill logic
     */
    private void refillTokens() {
        Instant now = Instant.now();
        Instant lastRefillTime = lastRefill.get();
        
        if (Duration.between(lastRefillTime, now).compareTo(refillInterval) >= 0) {
            if (lastRefill.compareAndSet(lastRefillTime, now)) {
                // Add one token if enough time has passed
                int currentTokens = tokens.get();
                if (currentTokens < maxTokens) {
                    tokens.compareAndSet(currentTokens, currentTokens + 1);
                }
            }
        }
    }
}