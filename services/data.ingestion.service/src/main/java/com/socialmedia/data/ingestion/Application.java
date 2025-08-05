package com.socialmedia.data.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.socialmedia.data.ingestion.config.RedditApiConfig;

@SpringBootApplication
@EnableConfigurationProperties(RedditApiConfig.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
