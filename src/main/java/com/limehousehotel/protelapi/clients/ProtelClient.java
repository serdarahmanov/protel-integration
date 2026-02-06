package com.limehousehotel.protelapi.clients;


import com.limehousehotel.protelapi.protelDtos.ReservationsResponseDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;

@Component
public class ProtelClient {

    private final WebClient webClient;

    public ProtelClient(WebClient protelWebClient) {
        this.webClient = protelWebClient;
    }

    public ReservationsResponseDto fetchReservations(OffsetDateTime modifiedAfter, Integer offsetId, int maxResponses) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/reservations") // adjust path
                            .queryParam("modifiedAfter", modifiedAfter.toString())
                            .queryParam("maxResponses", maxResponses);
                    if (offsetId != null) b.queryParam("offsetID", offsetId);
                    return b.build();
                })
                .retrieve()// Calls the endpoint
                .bodyToMono(ReservationsResponseDto.class) // I did not quite understand this part.
                .block();
    }

}
