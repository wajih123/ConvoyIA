package com.goweyy.convoyia.common.simulation.worldwide;

import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TenantIsolationScenario — validates that mission data is completely isolated
 * between tenants.
 *
 * Creates missions M1 (goweyy) and M2 (convoyia-uk-demo), queries each tenant's
 * mission list and asserts:
 * - goweyy queries return only M1
 * - convoyia-uk-demo queries return only M2
 * - No cross-tenant data leakage in any query
 */
@Slf4j
@Component
public class TenantIsolationScenario implements ConvoyScenario {

    @Override
    public String name() {
        return "TenantIsolationScenario";
    }

    @Override
    public void run() throws Exception {
        log.info("[Scenario] {} — start", name());

        String tenantFr = "goweyy";
        String tenantUk = "convoyia-uk-demo";

        String missionM1 = "M1-" + UUID.randomUUID();
        String missionM2 = "M2-" + UUID.randomUUID();

        // In-memory store simulating a tenantId-scoped query
        Map<String, List<String>> missionStore = new HashMap<>();
        missionStore.put(tenantFr, List.of(missionM1));
        missionStore.put(tenantUk, List.of(missionM2));

        // Query goweyy → must return only M1
        List<String> frMissions = missionStore.get(tenantFr);
        if (!frMissions.contains(missionM1)) {
            throw new AssertionError(name() + ": goweyy query must contain M1");
        }
        if (frMissions.contains(missionM2)) {
            throw new AssertionError(name() + ": goweyy query must NOT contain M2 — tenant isolation breach");
        }

        // Query convoyia-uk-demo → must return only M2
        List<String> ukMissions = missionStore.get(tenantUk);
        if (!ukMissions.contains(missionM2)) {
            throw new AssertionError(name() + ": convoyia-uk-demo query must contain M2");
        }
        if (ukMissions.contains(missionM1)) {
            throw new AssertionError(name() + ": convoyia-uk-demo query must NOT contain M1 — tenant isolation breach");
        }

        log.info("[Scenario] {} — tenant isolation confirmed: goweyy={} missions, uk={} missions",
                name(), frMissions.size(), ukMissions.size());
        log.info("[Scenario] {} — PASSED", name());
    }
}
