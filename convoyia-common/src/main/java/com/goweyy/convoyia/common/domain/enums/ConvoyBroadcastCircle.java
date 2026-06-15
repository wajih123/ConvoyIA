package com.goweyy.convoyia.common.domain.enums;

public enum ConvoyBroadcastCircle {
    CERCLE_1(5, 30),
    CERCLE_2(15, 30),
    CERCLE_3(30, 30),
    SURGE_1(30, 30),
    SURGE_2(30, 30);

    private final int radiusKm;
    private final int timeoutSec;

    ConvoyBroadcastCircle(int radiusKm, int timeoutSec) {
        this.radiusKm = radiusKm;
        this.timeoutSec = timeoutSec;
    }

    public int getRadiusKm() {
        return radiusKm;
    }

    public int getTimeoutSec() {
        return timeoutSec;
    }
}
