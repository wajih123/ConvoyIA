package com.goweyy.convoyia.dispatcher.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConvoyDispatchRequest {
    private String tenantId;
    private String originAddress;
    private String destinationAddress;
    private double vehicleDeclaredValue;
    private ConvoyUrgency urgency;
    private boolean clientAboard;
    private String clientName;
    private String vehiclePlate;
    private String vehicleBrand;
    private String vehicleModel;
}
