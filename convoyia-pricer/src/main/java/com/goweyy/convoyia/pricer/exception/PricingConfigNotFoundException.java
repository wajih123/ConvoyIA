package com.goweyy.convoyia.pricer.exception;

public class PricingConfigNotFoundException extends RuntimeException {
    public PricingConfigNotFoundException(String tenantId) {
        super("No active pricing configuration found for tenantId: " + tenantId);
    }
}
