// src/main/java/com/socialmedia/data/ingestion/config/WebClientConfig.java
package com.socialmedia.data.ingestion.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class WebClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);
    private final RedditApiConfig redditConfig;
    
    public WebClientConfig(RedditApiConfig redditConfig) {
        this.redditConfig = redditConfig;
    }
    
    @Bean
    public WebClient redditWebClient() {
        // Configure HTTP client with timeouts and connection pooling
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, redditConfig.getConnectionTimeoutMs())
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(redditConfig.getReadTimeoutMs(), TimeUnit.MILLISECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(redditConfig.getReadTimeoutMs(), TimeUnit.MILLISECONDS)))
            .compress(true); // Enable compression
        
        // Increase buffer size for large Reddit responses
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
        
        return WebClient.builder()
            .baseUrl(redditConfig.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .defaultHeader("User-Agent", redditConfig.getUserAgent())
            .filter(logRequest())
            .filter(logResponse())
            .filter(errorHandler())
            .build();
    }
    
    // Request logging filter
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            logger.debug("Reddit API Request: {} {}", 
                clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
    
    // Response logging filter
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            logger.debug("Reddit API Response: {} for {}", 
                clientResponse.statusCode(), clientResponse.request().getURI());
            return Mono.just(clientResponse);
        });
    }
    
    // Centralized error handling
    private ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().is4xxClientError() || 
                clientResponse.statusCode().is5xxServerError()) {
                
                logger.warn("Reddit API error response: {} for {}", 
                    clientResponse.statusCode(), clientResponse.request().getURI());
                
                // Don't throw here - let the service handle retries
                return Mono.just(clientResponse);
            }
            return Mono.just(clientResponse);
        });
    }
    
    @Bean
    public Retry redditApiRetrySpec() {
        return Retry.backoff(redditConfig.getMaxRetries(), 
                Duration.ofMillis(redditConfig.getRetryDelayMs()))
            .filter(throwable -> {
                // Retry on network issues and 5xx errors, not 4xx
                return !(throwable instanceof WebClientResponseException.BadRequest);
            })
            .doAfterRetry(retrySignal -> 
                logger.warn("Retrying Reddit API call, attempt: {}, error: {}", 
                    retrySignal.totalRetries() + 1, retrySignal.failure().getMessage()));
    }
}