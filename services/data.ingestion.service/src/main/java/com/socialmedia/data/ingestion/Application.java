package com.socialmedia.data.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.socialmedia.data.ingestion.config.RedditApiConfig;
import com.socialmedia.data.ingestion.config.YouTubeApiConfig;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableConfigurationProperties({RedditApiConfig.class, YouTubeApiConfig.class})
@EnableAsync
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
