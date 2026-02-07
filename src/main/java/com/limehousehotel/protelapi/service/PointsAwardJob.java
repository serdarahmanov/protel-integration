package com.limehousehotel.protelapi.service;

import com.limehousehotel.protelapi.clients.WordPressPointsClient;
import com.limehousehotel.protelapi.repos.ProtelStaysRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PointsAwardJob {

    private final ProtelStaysRepository staysRepo;
    private final WordPressPointsClient wpClient;

    public PointsAwardJob(
            ProtelStaysRepository staysRepo,
            WordPressPointsClient wpClient
    ) {
        this.staysRepo = staysRepo;
        this.wpClient = wpClient;
    }

    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void run() {
        var batch = staysRepo.findCompletedNotAwarded();
        if (batch.isEmpty()) return;

        for (var stay : batch) {
            try {
                var amountStr = (stay.amountSpent() == null) ? "0" : stay.amountSpent().toPlainString();
                var currency = (stay.currency() == null || stay.currency().isBlank()) ? "GBP" : stay.currency();

                wpClient.awardPoints(stay.wpUserId(), stay.reservationId(), amountStr, currency);

                // Prefer UTC for consistency
                staysRepo.markAwarded(stay.stayId(), Instant.now());

            } catch (Exception ex) {
                // log and continue; it will retry next run
                // log.warn("Failed awarding points for reservation {}: {}", stay.reservationId(), ex.getMessage(), ex);
            }
        }
    }
}
