package com.goweyy.convoyia.dispatcher.convoy.booking;

import lombok.Data;

@Data
public class ConvoyReturnBookingRequest {
    private String arrivalAddress;
    private String conveyorHomeCity;
}
