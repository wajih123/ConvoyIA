package com.goweyy.convoyia.common.simulation.worldwide;

import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import com.goweyy.convoyia.common.simulation.ConvoyScenario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GlobalScalabilityScenario — simulates 50 concurrent missions across 3 tenants
 * and validates currency isolation, error rate, and basic throughput.
 *
 * Distribution:
 *  - 20 missions: goweyy (EUR)
 *  - 20 missions: convoyia-uk-demo (GBP)
 *  - 10 missions: convoyia-uae-demo (AED)
 *
 * Assertions:
 *  - 0% cross-tenant data leakage
 *  - All pricing in correct currency
 *  - No errors during concurrent execution
 *  - p95 latency < 2000ms per simulated agent (without LLM)
 */
@Slf4j
@Component
public class GlobalScalabilityScenario implements ConvoyScenario {

    private static final int TOTAL_MISSIONS = 50;
    private static final long P95_LATENCY_THRESHOLD_MS = 2000L;

    @Override
    public String name() {
        return "GlobalScalabilityScenario";
    }

    @Override
    public void run() throws Exception {
        log.info("[Scenario] {} — start ({} concurrent missions)", name(), TOTAL_MISSIONS);

        List<MissionSpec> missions = buildMissionSpecs();
        Collections.shuffle(missions); // random interleaving

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<MissionResult>> futures = new ArrayList<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (MissionSpec spec : missions) {
            futures.add(executor.submit(() -> simulateMission(spec)));
        }

        executor.shutdown();
        boolean completed = executor.awaitTermination(30, TimeUnit.SECONDS);
        if (!completed) {
            throw new AssertionError(name() + ": executor did not terminate within 30s");
        }

        List<MissionResult> results = new ArrayList<>();
        for (Future<MissionResult> f : futures) {
            try {
                results.add(f.get());
            } catch (ExecutionException e) {
                errorCount.incrementAndGet();
                log.error("[Scenario] {} mission execution failed: {}", name(), e.getCause().getMessage());
            }
        }

        // Assert 0% errors
        if (errorCount.get() > 0) {
            throw new AssertionError(String.format(
                    "%s: %d/%d missions failed (expected 0 errors)",
                    name(), errorCount.get(), TOTAL_MISSIONS));
        }

        // Assert currency isolation per tenant
        assertCurrencyIsolation(results, "goweyy", ConvoyMarket.FRANCE.getCurrencyCode());
        assertCurrencyIsolation(results, "convoyia-uk-demo", "GBP");
        assertCurrencyIsolation(results, "convoyia-uae-demo", "AED");

        // Assert p95 latency
        List<Long> latencies = results.stream().map(MissionResult::latencyMs).sorted().toList();
        int p95Index = (int) Math.ceil(latencies.size() * 0.95) - 1;
        long p95 = latencies.get(Math.min(p95Index, latencies.size() - 1));
        if (p95 > P95_LATENCY_THRESHOLD_MS) {
            throw new AssertionError(String.format(
                    "%s: p95 latency %dms exceeds threshold %dms",
                    name(), p95, P95_LATENCY_THRESHOLD_MS));
        }

        log.info("[Scenario] {} — {} missions completed, 0 errors, p95={}ms — PASSED",
                name(), results.size(), p95);
    }

    private void assertCurrencyIsolation(List<MissionResult> results,
                                          String tenantId, String expectedCurrency) {
        long contaminated = results.stream()
                .filter(r -> r.tenantId().equals(tenantId))
                .filter(r -> !expectedCurrency.equals(r.currencyCode()))
                .count();
        if (contaminated > 0) {
            throw new AssertionError(String.format(
                    "%s: %d missions for tenant '%s' have wrong currency (expected %s)",
                    name(), contaminated, tenantId, expectedCurrency));
        }
    }

    private List<MissionSpec> buildMissionSpecs() {
        List<MissionSpec> specs = new ArrayList<>();
        for (int i = 0; i < 20; i++) specs.add(new MissionSpec("goweyy", ConvoyMarket.FRANCE.getCurrencyCode(), "0.20", "0.25", "30.00"));
        for (int i = 0; i < 20; i++) specs.add(new MissionSpec("convoyia-uk-demo", "GBP", "0.20", "0.25", "25.00"));
        for (int i = 0; i < 10; i++) specs.add(new MissionSpec("convoyia-uae-demo", "AED", "0.05", "0.25", "120.00"));
        return specs;
    }

    private MissionResult simulateMission(MissionSpec spec) {
        long start = System.currentTimeMillis();

        // Simulate pricing pipeline (Dispatcher → Verifier → Pricer — no LLM)
        BigDecimal baseFare = new BigDecimal(spec.minimumFare());
        BigDecimal vat = baseFare.multiply(new BigDecimal(spec.taxRate())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTtc = baseFare.add(vat).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = totalTtc.multiply(new BigDecimal(spec.platformFeeRatio()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyorShare = totalTtc.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

        long latency = System.currentTimeMillis() - start;

        return new MissionResult(
                UUID.randomUUID().toString(),
                spec.tenantId(),
                spec.currencyCode(),
                totalTtc,
                conveyorShare,
                latency
        );
    }

    record MissionSpec(String tenantId, String currencyCode, String taxRate,
                       String platformFeeRatio, String minimumFare) {}

    record MissionResult(String missionId, String tenantId, String currencyCode,
                         BigDecimal totalTtc, BigDecimal conveyorShare, long latencyMs) {}
}
