package com.limehousehotel.protelapi.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class WordPressPointsClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WordPressPointsClient(
            @Value("${wordpress.base-url}") String baseUrl,
            @Value("${wordpress.shared-secret}") String secret,
            @Value("${wordpress.timeout-seconds:15}") long timeoutSeconds,
            @Value("${wordpress.award-endpoint}") String awardEndpoint,
            ObjectMapper objectMapper
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Shared-Secret", secret)
                .build();
        this.objectMapper = objectMapper;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.awardEndpoint = awardEndpoint;
    }

    private final Duration timeout;
    private final String awardEndpoint;

    public AwardResponse awardPoints(long wpUserId, String reservationId, String amountSpent, String currency) {
        var body = new AwardRequest(wpUserId, reservationId, amountSpent, currency);

        return webClient.post()
                .uri(awardEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(rawBody -> parseResponse(
                                clientResponse.statusCode().value(),
                                rawBody,
                                wpUserId,
                                reservationId
                        )))
                .timeout(timeout)
                .block();
    }

    private AwardResponse parseResponse(int statusCode, String rawBody, long requestWpUserId, String requestReservationId) {
        boolean ok = statusCode >= 200 && statusCode < 300;
        String code = null;
        String message = null;
        boolean awarded = false;
        boolean alreadyAwarded = false;
        Integer points = null;
        Long wpUserId = null;
        String reservationId = null;
        Boolean staysUpdated = null;

        if (rawBody != null && !rawBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(rawBody);
                if (root.isObject()) {
                    if (root.has("ok")) ok = root.path("ok").asBoolean(ok);
                    code = textValue(root, "code");
                    String explicitMessage = textValue(root, "message");
                    String fallbackError = textValue(root, "error");
                    message = firstNonBlank(explicitMessage, fallbackError);
                    awarded = root.path("awarded").asBoolean(false);
                    alreadyAwarded = root.path("already_awarded").asBoolean(false);

                    if (root.has("points") && root.get("points").isNumber()) {
                        points = root.get("points").asInt();
                    }
                    if (root.has("wpUserId") && root.get("wpUserId").isNumber()) {
                        wpUserId = root.get("wpUserId").asLong();
                    }
                    reservationId = textValue(root, "reservationId");
                    if (root.has("stays_updated") && root.get("stays_updated").isBoolean()) {
                        staysUpdated = root.get("stays_updated").asBoolean();
                    }
                }
            } catch (Exception ignored) {
                // Keep raw body and derive a generic message below.
            }
        }

        if (wpUserId == null) wpUserId = requestWpUserId;
        if (reservationId == null || reservationId.isBlank()) reservationId = requestReservationId;
        if (code == null || code.isBlank()) code = ok ? "HTTP_" + statusCode : "WP_ERROR";
        if (message == null || message.isBlank()) message = "HTTP " + statusCode;

        return new AwardResponse(
                statusCode,
                ok,
                code,
                message,
                awarded,
                alreadyAwarded,
                points,
                wpUserId,
                reservationId,
                staysUpdated,
                rawBody
        );
    }

    private static String textValue(JsonNode root, String field) {
        if (!root.has(field)) return null;
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asText();
        return (text == null || text.isBlank()) ? null : text;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        if (fallback != null && !fallback.isBlank()) return fallback;
        return null;
    }

    public record AwardRequest(long wpUserId, String reservationId, String amountSpent, String currency) {}

    public record AwardResponse(
            int statusCode,
            boolean ok,
            String code,
            String message,
            boolean awarded,
            boolean alreadyAwarded,
            Integer points,
            Long wpUserId,
            String reservationId,
            Boolean staysUpdated,
            String rawBody
    ) {
        public boolean isHttpSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
