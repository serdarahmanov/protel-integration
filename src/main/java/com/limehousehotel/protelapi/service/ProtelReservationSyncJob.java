package com.limehousehotel.protelapi.service;

import com.limehousehotel.protelapi.DTO.ProtelStayPayload;
import com.limehousehotel.protelapi.clients.ProtelClient;
import com.limehousehotel.protelapi.protelDtos.ReservationDto;
import com.limehousehotel.protelapi.protelDtos.ReservationsResponseDto;
import com.limehousehotel.protelapi.repos.ProtelStaysRepository;
import com.limehousehotel.protelapi.repos.SyncStateRepository;
import com.limehousehotel.protelapi.repos.WpUserRepository;
import com.limehousehotel.protelapi.utility.DirectBookingChecker;
import com.limehousehotel.protelapi.utility.ReservationToStayMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ProtelReservationSyncJob {


    private final ProtelClient protelClient;
    private final SyncStateRepository syncStateRepo;
    private final WpUserRepository wpUserRepo;
    private final ProtelStaysRepository staysRepo;

    private final DirectBookingChecker directBookingChecker;


    private final int maxResponses;
    private final int overlapSeconds;


    public ProtelReservationSyncJob(
            DirectBookingChecker directBookingChecker,
            ProtelClient protelClient,
            SyncStateRepository syncStateRepo,
            WpUserRepository wpUserRepo,
            ProtelStaysRepository staysRepo,
            @Value("${protel.max-responses:500}") int maxResponses,
            @Value("${protel.overlap-seconds:120}") int overlapSeconds
    ) {
        this.directBookingChecker=directBookingChecker;
        this.protelClient = protelClient;
        this.syncStateRepo = syncStateRepo;
        this.wpUserRepo = wpUserRepo;
        this.staysRepo = staysRepo;
        this.maxResponses = maxResponses;
        this.overlapSeconds = overlapSeconds;
    }




    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void run() {

        var last = syncStateRepo.getLastModified("reservations");
        var safetyStart = last.minusSeconds(overlapSeconds);

        // Protel expects offset-date-time; choose UTC to be consistent
        var modifiedAfter = safetyStart.atOffset(ZoneOffset.UTC);

        Integer offsetId = null;
        LocalDateTime maxModifiedSeen = last;



        while (true) {


            ReservationsResponseDto resp = protelClient.fetchReservations(modifiedAfter, offsetId, maxResponses);
            if (resp == null || resp.getReservations() == null || resp.getReservations().isEmpty()) {
                break;
            }
            for (ReservationDto r : resp.getReservations()) {

                // 1) Keep ONLY direct bookings (based on segmentation.distributionChannel allowlist)
                if (!directBookingChecker.isDirectBooking(r)) {
                    continue;
                }



                // 2) Map minimal Protel DTO -> DB payload
                ProtelStayPayload payload = ReservationToStayMapper.toPayload(r);



                // Must have protelReservationId + email because your DB requires them (guest_email NOT NULL, unique id)
                if (payload.protelReservationId == null || payload.protelReservationId.isBlank()) {
                    continue;
                }
                if (payload.guestEmail == null || payload.guestEmail.isBlank()) {
                    continue;
                }


                // Normalize email for WP lookup
                String email = payload.guestEmail.trim().toLowerCase();



                // 3) Skip if email not linked to WP user (your rule)
                Long wpUserId = wpUserRepo.findUserIdByEmail(email);
                if (wpUserId == null) {
                    continue;
                }


                // 4) Track max modified timestamp for cursor update
                if (payload.modifiedAt != null && payload.modifiedAt.isAfter(maxModifiedSeen)) {
                    maxModifiedSeen = payload.modifiedAt;
                }



                // 5) Upsert into Xs0Jq_protel_stays
                //    (id is auto; unique is protel_reservation_id)
                staysRepo.upsertStay(
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




            }



            // Pagination handling using messageHeader.moreDataIndicator/moreDataOffsetID
            ReservationsResponseDto.MessageHeaderDto header = resp.getMessageHeader();
            if (header != null && Boolean.TRUE.equals(header.getMoreDataIndicator())) {
                offsetId = header.getMoreDataOffsetID();
            } else {
                break;
            }

        }

        // Update cursor ONLY after full pagination finished
        syncStateRepo.updateLastModified("reservations", maxModifiedSeen);
    }

}
