package com.goweyy.convoyia.inspector.convoy.dto;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConvoyDamageReport {
    private boolean damageDetected;
    private List<String> damagedZones;
    private ConvoyAlertSeverity severity;
    private String description;
}
