package com.goweyy.convoyia.common.simulation;

/**
 * Marker interface for all ConvoyIA simulation scenarios.
 *
 * Each implementation represents one end-to-end simulation that validates
 * a specific platform behaviour (pricing, tenant isolation, billing, etc.).
 *
 * Register scenarios with the simulation engine by implementing this interface.
 */
public interface ConvoyScenario {

    /** Human-readable name for reporting. */
    String name();

    /** Execute the scenario and throw an AssertionError on failure. */
    void run() throws Exception;
}
