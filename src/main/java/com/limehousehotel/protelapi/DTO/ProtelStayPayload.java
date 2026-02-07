package com.limehousehotel.protelapi.DTO;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public class ProtelStayPayload {

    public Long wpUserId;               // you will set by looking up WP users table
    public String guestEmail;
    public String protelReservationId;
    public LocalDate checkinDate;
    public LocalDate checkoutDate;
    public String roomType;
    public Integer adults;
    public Integer children;
    public BigDecimal amountSpent;
    public String currency;
    public String resStatus;
    public Instant modifiedAt;
}
