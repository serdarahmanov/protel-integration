package com.limehousehotel.protelapi.utility;

import com.limehousehotel.protelapi.config.DirectBookingConfig;
import com.limehousehotel.protelapi.protelDtos.ReservationDto;
import org.springframework.stereotype.Component;

@Component
public class DirectBookingChecker {
    private final DirectBookingConfig config;

    public DirectBookingChecker(DirectBookingConfig config) {
        this.config = config;
    }

    public boolean isDirectBooking(ReservationDto r) {
        if (r == null) return false;
        if (r.getSegmentation() == null) return false;

        String channel = r.getSegmentation().getDistributionChannel();
        return config.isDirectChannel(channel);
    }

}
