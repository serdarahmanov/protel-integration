package com.limehousehotel.protelapi.service;

import com.limehousehotel.protelapi.clients.WordPressPointsClient;
import com.limehousehotel.protelapi.repos.ProtelStaysRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsAwardJob {
    private static final Logger log = LoggerFactory.getLogger(PointsAwardJob.class);

    private final ProtelStaysRepository staysRepo;
    private final WordPressPointsClient wpClient;

    public PointsAwardJob(
            ProtelStaysRepository staysRepo,
            WordPressPointsClient wpClient
    ) {
        this.staysRepo = staysRepo;
        this.wpClient = wpClient;
    }

    @Scheduled(fixedDelayString = "${protel.award-delay:PT5M}")
    @Transactional
    public void run() {
        var batch = staysRepo.findCompletedNotAwarded();
        if (batch.isEmpty()) return;

        for (var stay : batch) {
            try {
                var amountStr = (stay.amountSpent() == null) ? "0" : stay.amountSpent().toPlainString();
                var currency = (stay.currency() == null || stay.currency().isBlank()) ? "GBP" : stay.currency();
                var response = wpClient.awardPoints(stay.wpUserId(), stay.reservationId(), amountStr, currency);

                if (response.isHttpSuccess() && response.ok() && response.awarded()) {
                    log.info(
                            "WordPress points awarded reservationId={} wpUserId={} points={} staysUpdated={} code={} message={}",
                            response.reservationId(),
                            response.wpUserId(),
                            response.points(),
                            response.staysUpdated(),
                            response.code(),
                            response.message()
                    );
                } else if (response.isHttpSuccess() && response.ok() && response.alreadyAwarded()) {
                    log.info(
                            "WordPress points already awarded reservationId={} wpUserId={} points={} staysUpdated={} code={} message={}",
                            response.reservationId(),
                            response.wpUserId(),
                            response.points(),
                            response.staysUpdated(),
                            response.code(),
                            response.message()
                    );
                } else if (response.isHttpSuccess()) {
                    log.warn(
                            "WordPress returned 2xx with unexpected body reservationId={} wpUserId={} status={} code={} message={} rawBody={}",
                            response.reservationId(),
                            response.wpUserId(),
                            response.statusCode(),
                            response.code(),
                            response.message(),
                            response.rawBody()
                    );
                } else {
                    log.error(
                            "WordPress award request failed reservationId={} wpUserId={} status={} code={} message={} rawBody={}",
                            response.reservationId(),
                            response.wpUserId(),
                            response.statusCode(),
                            response.code(),
                            response.message(),
                            response.rawBody()
                    );
                }

            } catch (Exception ex) {
                // log and continue; it will retry next run
                log.error("WordPress call threw exception reservationId={} wpUserId={} error={}",
                        stay.reservationId(), stay.wpUserId(), ex.getMessage(), ex);
            }
        }
    }
}
