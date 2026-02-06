package com.limehousehotel.protelapi.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class ProtelClientConfig {
//Configuration class to build request headers and define url

    @Bean
    WebClient protelWebClient(
            @Value("${protel.base-url}") String baseUrl,
            @Value("${protel.api-key}") String apiKey
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }
}
