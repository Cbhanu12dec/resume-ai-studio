package com.resumeai.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${anthropic.api-version:2023-06-01}")
    private String apiVersion;

    @Bean
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", apiVersion)
                .defaultHeader("content-type", "application/json")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(25 * 1024 * 1024)) // 25MB
                .build();
    }
}
