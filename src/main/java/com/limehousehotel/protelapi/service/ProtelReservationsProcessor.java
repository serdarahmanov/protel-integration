package com.limehousehotel.protelapi.service;

import com.limehousehotel.protelapi.DTO.ProtelStayPayload;
import com.limehousehotel.protelapi.protelDtos.ReservationDto;
import com.limehousehotel.protelapi.protelDtos.ReservationsResponseDto;
import com.limehousehotel.protelapi.utility.DirectBookingChecker;
import com.limehousehotel.protelapi.utility.ReservationToStayMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProtelReservationsProcessor {


    private static final Logger log = LoggerFactory.getLogger(ProtelReservationsProcessor.class);


    private final DirectBookingChecker directBookingChecker;


    public ProtelReservationsProcessor(DirectBookingChecker directBookingChecker) {
        this.directBookingChecker = directBookingChecker;
    }

    public List<ProtelStayPayload> extractDirectStays(ReservationsResponseDto response) {
        List<ProtelStayPayload> out = new ArrayList<>();

        if (response == null || response.getReservations() == null) {
            return out;
        }

        for (ReservationDto r : response.getReservations()) {
            if (!directBookingChecker.isDirectBooking(r)) {
                continue;
            }

            ProtelStayPayload payload = ReservationToStayMapper.toPayload(r);

            // enforce your DB constraints early
            if (payload.protelReservationId == null || payload.protelReservationId.isBlank()) {
                log.warn("Skipping reservation: missing protelReservationId");
                continue;
            }
            if (payload.guestEmail == null || payload.guestEmail.isBlank()) {
                log.warn("Skipping reservation {}: missing guest email", payload.protelReservationId);
                continue;
            }
            if (payload.checkinDate == null || payload.checkoutDate == null) {
                log.warn("Skipping reservation {}: missing dates", payload.protelReservationId);
                continue;
            }

            out.add(payload);
        }

        return out;


    }


}
