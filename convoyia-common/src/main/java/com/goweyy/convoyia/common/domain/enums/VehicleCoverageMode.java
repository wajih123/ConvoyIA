package com.goweyy.convoyia.common.domain.enums;

public enum VehicleCoverageMode {
    PER_MISSION,   // calculated per mission by vehicle value tier
    INCLUDED,      // flat — absorbed in platform fee, not a separate line
    EXTERNAL       // tenant manages own vehicle insurance externally
}
