package com.goweyy.convoyia.dispatcher.convoy.booking;

import com.goweyy.convoyia.common.domain.enums.ConvoyReturnMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConvoyReturnBookingResult {
    private String missionId;
    private ConvoyReturnMode mode;
    private String originAddress;
    private String destinationAddress;
    private double distanceKm;
    private BigDecimal estimatedCost;
    private String status;
}
