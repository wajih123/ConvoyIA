package com.goweyy.convoyia.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ollama.base-url:http://ollama-service:11434}")
    private String ollamaBaseUrl;

    @Value("${hiscox.base-url:http://hiscox-api}")
    private String hiscoxBaseUrl;

    @Bean(name = "ollamaWebClient")
    public WebClient ollamaWebClient(WebClient.Builder builder) {
        return builder.baseUrl(ollamaBaseUrl).build();
    }

    @Bean(name = "hiscoxWebClient")
    public WebClient hiscoxWebClient(WebClient.Builder builder) {
        return builder.baseUrl(hiscoxBaseUrl).build();
    }
}
