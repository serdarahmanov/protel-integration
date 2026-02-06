package com.limehousehotel.protelapi.protelDtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationsResponseDto {

    private List<ReservationDto> reservations;
    private ProcessReferencesDto processReferences;
    private MessageHeaderDto messageHeader;

    public List<ReservationDto> getReservations() { return reservations; }
    public void setReservations(List<ReservationDto> reservations) { this.reservations = reservations; }

    public ProcessReferencesDto getProcessReferences() { return processReferences; }
    public void setProcessReferences(ProcessReferencesDto processReferences) { this.processReferences = processReferences; }

    public MessageHeaderDto getMessageHeader() { return messageHeader; }
    public void setMessageHeader(MessageHeaderDto messageHeader) { this.messageHeader = messageHeader; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessReferencesDto {
        private List<Object> warnings;
        private List<Object> infos;

        public List<Object> getWarnings() { return warnings; }
        public void setWarnings(List<Object> warnings) { this.warnings = warnings; }

        public List<Object> getInfos() { return infos; }
        public void setInfos(List<Object> infos) { this.infos = infos; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageHeaderDto {
        // e.g. "2026-01-17"
        private String currentBookingDate;
        private Boolean moreDataIndicator;
        private Integer moreDataOffsetID;

        public String getCurrentBookingDate() { return currentBookingDate; }
        public void setCurrentBookingDate(String currentBookingDate) { this.currentBookingDate = currentBookingDate; }

        public Boolean getMoreDataIndicator() { return moreDataIndicator; }
        public void setMoreDataIndicator(Boolean moreDataIndicator) { this.moreDataIndicator = moreDataIndicator; }

        public Integer getMoreDataOffsetID() { return moreDataOffsetID; }
        public void setMoreDataOffsetID(Integer moreDataOffsetID) { this.moreDataOffsetID = moreDataOffsetID; }
    }





}
