package com.limehousehotel.protelapi.utility;


import com.limehousehotel.protelapi.DTO.ProtelStayPayload;
import com.limehousehotel.protelapi.protelDtos.ReservationDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public class ReservationToStayMapper {

    private ReservationToStayMapper() {}



    public static ProtelStayPayload toPayload(ReservationDto r) {
        ProtelStayPayload out = new ProtelStayPayload();

        out.protelReservationId = extractReservationId(r);
        out.guestEmail = extractEmail(r);

        out.checkinDate = (r.getArrivalDate() != null) ? r.getArrivalDate().toLocalDate() : null;
        out.checkoutDate = (r.getDepartureDate() != null) ? r.getDepartureDate().toLocalDate() : null;

        out.roomType = extractRoomType(r);

        out.adults = extractGuestCount(r, "Adult");
        out.children = extractGuestCount(r, "Child");

        out.amountSpent = sumAmountSpent(r);
        out.currency = extractCurrency(r);

        out.resStatus = r.getResStatus();

        out.modifiedAt = (r.getLastModifyDateTime() != null)
                ? LocalDateTime.ofInstant(r.getLastModifyDateTime().toInstant(), ZoneOffset.UTC)
                : null;

        return out;
    }

    // Prefer idContext="protelIO" else fallback to first id
    private static String extractReservationId(ReservationDto r) {

        if (r == null || r.getReservationIds() == null || r.getReservationIds().isEmpty()) return null;

        return r.getReservationIds().stream()
                .filter(x -> x != null && x.getId() != null && !x.getId().isBlank())
                .filter(x -> "protelIO".equalsIgnoreCase(x.getIdContext()))
                .map(ReservationDto.ReservationIdDto::getId)
                .findFirst()
                .orElseGet(() ->
                        r.getReservationIds().stream()
                                .filter(x -> x != null && x.getId() != null && !x.getId().isBlank())
                                .map(ReservationDto.ReservationIdDto::getId)
                                .findFirst()
                                .orElse(null)
                );
    }

    // 1) bookerProfile email
    // 2) fallback: primary guest email
    private static String extractEmail(ReservationDto r) {
        if (r != null
                && r.getBookerProfile() != null
                && r.getBookerProfile().getProfile() != null
                && r.getBookerProfile().getProfile().getContact() != null) {
            String email = r.getBookerProfile().getProfile().getContact().getEmail();
            if (email != null && !email.isBlank()) return email.trim();
        }

        if (r != null && r.getGuestProfiles() != null) {
            return r.getGuestProfiles().stream()
                    .filter(g -> g != null && Boolean.TRUE.equals(g.getPrimaryInd()))
                    .map(g -> g.getProfile() != null && g.getProfile().getContact() != null ? g.getProfile().getContact().getEmail() : null)
                    .filter(e -> e != null && !e.isBlank())
                    .map(String::trim)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private static Integer extractGuestCount(ReservationDto r, String code) {
        if (r == null || r.getGuestCounts() == null) return null;

        return r.getGuestCounts().stream()
                .filter(gc -> gc != null && code.equalsIgnoreCase(gc.getAgeQualifyingCode()))
                .map(ReservationDto.GuestCountDto::getCount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // room_type from first slice with room: roomTypePreset else roomType
    private static String extractRoomType(ReservationDto r) {
        if (r == null || r.getSlices() == null) return null;

        return r.getSlices().stream()
                .filter(s -> s != null && s.getRoom() != null)
                .map(s -> {
                    String preset = s.getRoom().getRoomTypePreset();
                    if (preset != null && !preset.isBlank()) return preset;
                    String type = s.getRoom().getRoomType();
                    return (type != null && !type.isBlank()) ? type : null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // amount_spent = sum of slice.rate.price.singleAmountAfterTax
    private static BigDecimal sumAmountSpent(ReservationDto r) {
        if (r == null || r.getSlices() == null) return BigDecimal.ZERO;

        return r.getSlices().stream()
                .filter(s -> s != null && s.getRate() != null && s.getRate().getPrice() != null)
                .map(s -> s.getRate().getPrice().getSingleAmountAfterTax())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String extractCurrency(ReservationDto r) {
        if (r == null || r.getSlices() == null) return null;

        return r.getSlices().stream()
                .filter(s -> s != null && s.getRate() != null && s.getRate().getPrice() != null)
                .map(s -> s.getRate().getPrice().getCurrencyCode())
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse(null);
    }

}
