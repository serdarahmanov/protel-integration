package com.limehousehotel.protelapi.protelDtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationDto {

    // for modified_at
    private OffsetDateTime lastModifyDateTime;

    // for res_status
    private String resStatus;

    // for protel_reservation_id
    private List<ReservationIdDto> reservationIds;

    // for checkin_date / checkout_date
    private OffsetDateTime arrivalDate;
    private OffsetDateTime departureDate;

    // for adults / children
    private List<GuestCountDto> guestCounts;

    // for guest_email
    private BookerProfileDto bookerProfile;

    // fallback for guest_email if bookerProfile missing email
    private List<GuestProfileDto> guestProfiles;

    // for amount_spent / currency / room_type
    private List<SliceDto> slices;

    // for direct booking filter
    private SegmentationDto segmentation;

    public OffsetDateTime getLastModifyDateTime() { return lastModifyDateTime; }
    public void setLastModifyDateTime(OffsetDateTime lastModifyDateTime) { this.lastModifyDateTime = lastModifyDateTime; }

    public String getResStatus() { return resStatus; }
    public void setResStatus(String resStatus) { this.resStatus = resStatus; }

    public List<ReservationIdDto> getReservationIds() { return reservationIds; }
    public void setReservationIds(List<ReservationIdDto> reservationIds) { this.reservationIds = reservationIds; }

    public OffsetDateTime getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(OffsetDateTime arrivalDate) { this.arrivalDate = arrivalDate; }

    public OffsetDateTime getDepartureDate() { return departureDate; }
    public void setDepartureDate(OffsetDateTime departureDate) { this.departureDate = departureDate; }

    public List<GuestCountDto> getGuestCounts() { return guestCounts; }
    public void setGuestCounts(List<GuestCountDto> guestCounts) { this.guestCounts = guestCounts; }

    public BookerProfileDto getBookerProfile() { return bookerProfile; }
    public void setBookerProfile(BookerProfileDto bookerProfile) { this.bookerProfile = bookerProfile; }

    public List<GuestProfileDto> getGuestProfiles() { return guestProfiles; }
    public void setGuestProfiles(List<GuestProfileDto> guestProfiles) { this.guestProfiles = guestProfiles; }

    public List<SliceDto> getSlices() { return slices; }
    public void setSlices(List<SliceDto> slices) { this.slices = slices; }

    public SegmentationDto getSegmentation() { return segmentation; }
    public void setSegmentation(SegmentationDto segmentation) { this.segmentation = segmentation; }

    // ---------- nested DTOs ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReservationIdDto {

        private String id;
        private String idContext;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getIdContext() { return idContext; }
        public void setIdContext(String idContext) { this.idContext = idContext; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentationDto {
        private String distributionChannel;

        public String getDistributionChannel() { return distributionChannel; }
        public void setDistributionChannel(String distributionChannel) { this.distributionChannel = distributionChannel; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GuestCountDto {
        private String ageQualifyingCode; // "Adult", "Child"
        private Integer count;

        public String getAgeQualifyingCode() { return ageQualifyingCode; }
        public void setAgeQualifyingCode(String ageQualifyingCode) { this.ageQualifyingCode = ageQualifyingCode; }

        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookerProfileDto {
        private ProfileDto profile;

        public ProfileDto getProfile() { return profile; }
        public void setProfile(ProfileDto profile) { this.profile = profile; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GuestProfileDto {
        private ProfileDto profile;
        private Boolean primaryInd;

        public ProfileDto getProfile() { return profile; }
        public void setProfile(ProfileDto profile) { this.profile = profile; }

        public Boolean getPrimaryInd() { return primaryInd; }
        public void setPrimaryInd(Boolean primaryInd) { this.primaryInd = primaryInd; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileDto {
        private ContactDto contact;

        public ContactDto getContact() { return contact; }
        public void setContact(ContactDto contact) { this.contact = contact; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactDto {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SliceDto {
        private RateDto rate;
        private RoomDto room;

        public RateDto getRate() { return rate; }
        public void setRate(RateDto rate) { this.rate = rate; }

        public RoomDto getRoom() { return room; }
        public void setRoom(RoomDto room) { this.room = room; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RateDto {
        private PriceDto price;

        public PriceDto getPrice() { return price; }
        public void setPrice(PriceDto price) { this.price = price; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceDto {
        private String currencyCode;
        private BigDecimal singleAmountAfterTax;

        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

        public BigDecimal getSingleAmountAfterTax() { return singleAmountAfterTax; }
        public void setSingleAmountAfterTax(BigDecimal singleAmountAfterTax) { this.singleAmountAfterTax = singleAmountAfterTax; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoomDto {
        private String roomTypePreset;
        private String roomType;

        public String getRoomTypePreset() { return roomTypePreset; }
        public void setRoomTypePreset(String roomTypePreset) { this.roomTypePreset = roomTypePreset; }

        public String getRoomType() { return roomType; }
        public void setRoomType(String roomType) { this.roomType = roomType; }
    }


}

