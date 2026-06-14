package com.goweyy.convoyia.inspector.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DamageReport {
    List<String> areas;
    String severity;
    List<String> newDamagesVsDeparture;
    boolean requiresHiscoxAlert;
    List<String> photoUrls;
}
