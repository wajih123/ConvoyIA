package com.goweyy.convoyia.common.domain.enums;

public enum ConvoyVehicleSegment {
    STANDARD(0, 20_000),
    COURANT(20_000, 40_000),
    PREMIUM(40_000, 70_000),
    HAUT_DE_GAMME(70_000, 120_000),
    LUXE_PLATEAU(120_000, Integer.MAX_VALUE);

    private final int minValue;
    private final int maxValue;

    ConvoyVehicleSegment(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public int getMinValue() { return minValue; }
    public int getMaxValue() { return maxValue; }

    public static ConvoyVehicleSegment fromValue(double vehicleValue) {
        for (ConvoyVehicleSegment s : values()) {
            if (vehicleValue >= s.minValue && vehicleValue < s.maxValue) {
                return s;
            }
        }
        return LUXE_PLATEAU;
    }
}
