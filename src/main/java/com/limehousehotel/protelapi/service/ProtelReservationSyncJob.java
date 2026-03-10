package com.limehousehotel.protelapi.service;

import com.limehousehotel.protelapi.DTO.ProtelStayPayload;
import com.limehousehotel.protelapi.clients.ProtelClient;
import com.limehousehotel.protelapi.protelDtos.ReservationsResponseDto;
import com.limehousehotel.protelapi.repos.ProtelStaysRepository;
import com.limehousehotel.protelapi.repos.SyncStateRepository;
import com.limehousehotel.protelapi.repos.WpUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
public class ProtelReservationSyncJob {
    private static final Logger log = LoggerFactory.getLogger(ProtelReservationSyncJob.class);

    private final ProtelReservationsProcessor reservationsProcessor;
    private final ProtelClient protelClient;
    private final SyncStateRepository syncStateRepo;
    private final WpUserRepository wpUserRepo;
    private final ProtelStaysRepository staysRepo;

    private final int maxResponses;
    private final int overlapSeconds;


    public ProtelReservationSyncJob(
            ProtelReservationsProcessor reservationsProcessor,
            ProtelClient protelClient,
            SyncStateRepository syncStateRepo,
            WpUserRepository wpUserRepo,
            ProtelStaysRepository staysRepo,
            @Value("${protel.max-responses:100}") int maxResponses,
            @Value("${protel.overlap-seconds:120}") int overlapSeconds
    ) {
        this.reservationsProcessor = reservationsProcessor;
        this.protelClient = protelClient;
        this.syncStateRepo = syncStateRepo;
        this.wpUserRepo = wpUserRepo;
        this.staysRepo = staysRepo;
        this.maxResponses = maxResponses;
        this.overlapSeconds = overlapSeconds;
    }




    @Scheduled(fixedDelayString = "${protel.sync-delay:PT5M}")
    @Transactional
    public void run() {

        var last = syncStateRepo.getLastModified("reservations");
        var safetyStart = last.minusSeconds(overlapSeconds);

        // Protel expects offset-date-time; choose UTC to be consistent
        var modifiedAfter = safetyStart.atOffset(ZoneOffset.UTC);

        Integer offsetId = null;
        int repeatedOffsetCount = 0;
        Instant maxModifiedSeen = last;



        while (true) {


            ReservationsResponseDto resp = protelClient.fetchReservations(modifiedAfter, offsetId, maxResponses);
            if (resp == null || resp.getReservations() == null || resp.getReservations().isEmpty()) {
                break;
            }
           // 1) Track max modified timestamp for cursor update
            for (var reservation : resp.getReservations()) {
                var modifiedAt = reservation.getLastModifyDateTime();
                if (modifiedAt == null) {
                    continue;
                }
                var modifiedInstant = modifiedAt.toInstant();
                if (modifiedInstant.isAfter(maxModifiedSeen)) {
                    maxModifiedSeen = modifiedInstant;
                }


            }

            for (ProtelStayPayload payload : reservationsProcessor.extractDirectStays(resp)) {



                // Normalize email for WP lookup
                String email = payload.guestEmail.trim().toLowerCase();
                // 3) Skip if email not linked to WP user (your rule)
                Long wpUserId = wpUserRepo.findUserIdByEmail(email);

                if (wpUserId == null) {
                    log.info(
                            "Skipping reservation {}: guest email not linked to WordPress user ({}) distributionChannel={}",
                            payload.protelReservationId,
                            email,
                            payload.distributionChannel
                    );
                    continue;
                }




                // 5) Upsert into Xs0Jq_protel_stays
                //    (id is auto; unique is protel_reservation_id)
                var upsertResult = staysRepo.upsertStay(
                        payload.protelReservationId,
                        wpUserId,
                        email,
                        payload.checkinDate,
                        payload.checkoutDate,
                        payload.roomType,
                        payload.adults,
                        payload.children,
                        payload.amountSpent != null ? payload.amountSpent : BigDecimal.ZERO,
                        payload.currency,
                        payload.resStatus,
                        payload.modifiedAt
                );

                if (upsertResult.action() == ProtelStaysRepository.StayUpsertAction.INSERTED) {
                    log.info(
                            "Inserted stay reservationId={} wpUserId={} email={} distributionChannel={} checkin={} checkout={} roomType={} adults={} children={} amountSpent={} currency={} resStatus={} modifiedAt={}",
                            payload.protelReservationId,
                            wpUserId,
                            email,
                            payload.distributionChannel,
                            payload.checkinDate,
                            payload.checkoutDate,
                            payload.roomType,
                            payload.adults,
                            payload.children,
                            payload.amountSpent != null ? payload.amountSpent : BigDecimal.ZERO,
                            payload.currency,
                            payload.resStatus,
                            payload.modifiedAt
                    );
                } else if (upsertResult.action() == ProtelStaysRepository.StayUpsertAction.UPDATED) {
                    log.info(
                            "Updated stay reservationId={} wpUserId={} email={} distributionChannel={} checkin={} checkout={} roomType={} adults={} children={} amountSpent={} currency={} resStatus={} modifiedAt={}",
                            payload.protelReservationId,
                            wpUserId,
                            email,
                            payload.distributionChannel,
                            payload.checkinDate,
                            payload.checkoutDate,
                            payload.roomType,
                            payload.adults,
                            payload.children,
                            payload.amountSpent != null ? payload.amountSpent : BigDecimal.ZERO,
                            payload.currency,
                            payload.resStatus,
                            payload.modifiedAt
                    );
                }




            }



            // Pagination handling using messageHeader.moreDataIndicator/moreDataOffsetID
            ReservationsResponseDto.MessageHeaderDto header = resp.getMessageHeader();
            if (header != null && Boolean.TRUE.equals(header.getMoreDataIndicator())) {
                Integer nextOffsetId = header.getMoreDataOffsetID();
                if (nextOffsetId == null) {
                    log.error("Stopping pagination: moreDataIndicator=true but moreDataOffsetID is null");
                    break;
                }
                if (offsetId != null && offsetId.equals(nextOffsetId)) {
                    repeatedOffsetCount++;
                    if (repeatedOffsetCount > 3) {
                        log.error("Stopping pagination: offsetID repeated more than 3 times (offsetID={}, repeats={})",
                                nextOffsetId, repeatedOffsetCount);
                        break;
                    }
                    log.warn("OffsetID repeated (offsetID={}, repeats={}); continuing", nextOffsetId, repeatedOffsetCount);
                } else {
                    repeatedOffsetCount = 0;
                }
                offsetId = nextOffsetId;
            } else {
                break;
            }

        }

        // Update cursor ONLY after full pagination finished
        syncStateRepo.updateLastModified("reservations", maxModifiedSeen);
    }

}
