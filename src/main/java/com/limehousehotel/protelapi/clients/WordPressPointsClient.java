package com.limehousehotel.protelapi.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class WordPressPointsClient {
    private final WebClient webClient;

    public WordPressPointsClient(
            @Value("${wordpress.base-url}") String baseUrl,
            @Value("${wordpress.shared-secret}") String secret,
            @Value("${wordpress.timeout-seconds:15}") long timeoutSeconds,
            @Value("${wordpress.award-endpoint}") String awardEndpoint
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Shared-Secret", secret)
                .build();

        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.awardEnpoint=awardEndpoint;
    }

    private final Duration timeout;
    private final String awardEnpoint;

    public void awardPoints(long wpUserId, String reservationId, String amountSpent, String currency) {
        var body = new AwardRequest(wpUserId, reservationId, amountSpent, currency);

        webClient.post()
                .uri(awardEnpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(timeout)
                .block();
    }

    public record AwardRequest(long wpUserId, String reservationId, String amountSpent, String currency) {}
}
