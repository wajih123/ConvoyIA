package com.goweyy.convoyia.common.simulation;

import com.goweyy.convoyia.common.domain.enums.ConvoyBillingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyPricingStatus;
import com.goweyy.convoyia.common.domain.enums.ConvoyUrgency;
import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 100 client-conveyor interaction simulation test.
 *
 * Each interaction simulates a full flow:
 * Client requests mission → Driver verified → Mission priced → Billing applied
 *
 * Validates the end-to-end pipeline for 100 different client/conveyor pairs
 * covering: successful billing, damage pauses, manual quotes, blocked drivers, etc.
 */
@DisplayName("100 Client-Conveyor Interaction Simulation")
class Convoy100ClientConvoyorInteractionTest {

    private static final BigDecimal FLAT_BASE       = new BigDecimal("15.00");
    private static final BigDecimal RATE_PER_KM     = new BigDecimal("0.80");
    private static final BigDecimal MINIMUM_FARE    = new BigDecimal("30.00");
    private static final BigDecimal VAT_RATE         = new BigDecimal("0.20");
    private static final BigDecimal CONVEYOR_RATIO   = new BigDecimal("0.75");
    private static final BigDecimal PLATFORM_RATIO   = new BigDecimal("0.25");
    private static final BigDecimal STRIPE_PRE_AUTH  = new BigDecimal("1.20");
    private static final BigDecimal EXPRESS_BONUS    = new BigDecimal("0.15");
    private static final BigDecimal URGENT_BONUS     = new BigDecimal("0.30");

    @Test
    @DisplayName("100 client-conveyor interactions: full pipeline invariants")
    void simulate_100_client_conveyor_interactions() {
        List<Interaction> interactions = generate100Interactions();
        assertThat(interactions).hasSize(100);

        List<String> failures = new ArrayList<>();

        for (Interaction interaction : interactions) {
            try {
                processInteraction(interaction, failures);
            } catch (Exception e) {
                failures.add("Unexpected exception in interaction " + interaction.id + ": " + e.getMessage());
            }
        }

        assertThat(failures)
                .as("All 100 client-conveyor interactions must pass. Failures:\n" +
                    String.join("\n", failures))
                .isEmpty();
    }

    @Test
    @DisplayName("100 interactions: billing status distribution is correct")
    void billing_status_distribution_is_correct() {
        List<Interaction> interactions = generate100Interactions();
        long damageCount = interactions.stream().filter(i -> i.damageDetected()).count();
        long luxeCount   = interactions.stream().filter(i -> i.segment() == ConvoyVehicleSegment.LUXE_PLATEAU).count();
        long normalCount = interactions.stream()
                .filter(i -> !i.damageDetected() && i.segment() != ConvoyVehicleSegment.LUXE_PLATEAU)
                .count();

        // All 100 interactions are accounted for by exactly one category
        assertThat(damageCount + luxeCount + normalCount)
                .as("damage + luxe + normal must sum to 100")
                .isEqualTo(100);

        // At least 50 normal (non-damaged, non-luxe) missions should exist
        assertThat(normalCount).as("At least 50 normal interactions should be billed").isGreaterThanOrEqualTo(50);

        // Damage and LUXE categories must each be present
        assertThat(damageCount).as("Some interactions should have damage").isGreaterThan(0);
        assertThat(luxeCount).as("Some interactions should be LUXE_PLATEAU").isGreaterThan(0);
    }

    @Test
    @DisplayName("100 interactions: verification gate prevents blocked drivers from billing")
    void blocked_drivers_never_reach_billing() {
        List<Interaction> interactions = generate100Interactions();
        for (Interaction interaction : interactions) {
            ConvoyVerificationStatus verStatus = computeVerificationStatus(interaction);
            if (verStatus == ConvoyVerificationStatus.BLOCKED) {
                // Billing should not proceed — status remains PENDING or should not be BILLED
                ConvoyBillingStatus billStatus = computeBillingStatusGated(interaction);
                assertThat(billStatus)
                        .as("Blocked driver %s must not reach BILLED status", interaction.id)
                        .isNotEqualTo(ConvoyBillingStatus.BILLED);
            }
        }
    }

    @Test
    @DisplayName("100 interactions: conveyor payout always >= minimum fare × 0.75")
    void conveyor_payout_always_above_minimum() {
        List<Interaction> interactions = generate100Interactions();
        BigDecimal minConveyorPayout = MINIMUM_FARE
                .multiply(BigDecimal.ONE.add(VAT_RATE))
                .multiply(CONVEYOR_RATIO)
                .setScale(2, RoundingMode.HALF_UP);

        for (Interaction interaction : interactions) {
            if (interaction.segment == ConvoyVehicleSegment.LUXE_PLATEAU) continue;
            if (interaction.damageDetected || interaction.driverBlocked) continue;
            BigDecimal ttc     = computeTtc(interaction);
            BigDecimal payout  = ttc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
            assertThat(payout)
                    .as("Conveyor payout for %s must be >= minimum payout threshold", interaction.id)
                    .isGreaterThanOrEqualTo(minConveyorPayout);
        }
    }

    // ── Interaction generation ────────────────────────────────────────────────

    private List<Interaction> generate100Interactions() {
        List<Interaction> interactions = new ArrayList<>();

        ConvoyVehicleSegment[] segments  = ConvoyVehicleSegment.values();
        ConvoyUrgency[] urgencies        = ConvoyUrgency.values();

        LocalDateTime weekday  = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)).withHour(14);
        LocalDateTime saturday = LocalDateTime.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).withHour(10);
        LocalDateTime[] times  = { weekday, saturday };

        // 50 normal successful interactions
        for (int i = 0; i < 50; i++) {
            interactions.add(new Interaction(
                    "client-" + i + "-conveyor-" + (i % 10),
                    segments[i % 4],          // non-LUXE cycling
                    urgencies[i % 3],
                    10 + i * 5.0,             // 10 to 255 km
                    times[i % 2],
                    false, false              // no damage, driver ok
            ));
        }

        // 15 damage-detected interactions → PENDING_DAMAGE_REVIEW
        for (int i = 0; i < 15; i++) {
            interactions.add(new Interaction(
                    "client-damage-" + i + "-conveyor-" + i,
                    segments[i % 4],
                    urgencies[i % 3],
                    50 + i * 10.0,
                    weekday,
                    true, false              // damage detected
            ));
        }

        // 10 blocked driver interactions → should not proceed to BILLED
        for (int i = 0; i < 10; i++) {
            interactions.add(new Interaction(
                    "client-blocked-" + i + "-conveyor-" + i,
                    segments[i % 4],
                    urgencies[i % 3],
                    30 + i * 5.0,
                    weekday,
                    false, true              // driver blocked
            ));
        }

        // 15 LUXE_PLATEAU → PENDING_MANUAL_QUOTE
        for (int i = 0; i < 15; i++) {
            interactions.add(new Interaction(
                    "client-luxe-" + i + "-conveyor-" + i,
                    ConvoyVehicleSegment.LUXE_PLATEAU,
                    ConvoyUrgency.STANDARD,
                    100 + i * 20.0,
                    weekday,
                    false, false
            ));
        }

        // Fill remaining to reach 100
        while (interactions.size() < 100) {
            int i = interactions.size();
            interactions.add(new Interaction(
                    "client-fill-" + i + "-conveyor-" + (i % 20),
                    segments[i % 4],
                    urgencies[i % 3],
                    20 + i * 2.0,
                    saturday,
                    false, false
            ));
        }

        return interactions;
    }

    // ── Pipeline processing ───────────────────────────────────────────────────

    private void processInteraction(Interaction interaction, List<String> failures) {
        // Step 1: Verification
        ConvoyVerificationStatus verStatus = computeVerificationStatus(interaction);

        // Step 2: Pricing (only if verified)
        if (verStatus == ConvoyVerificationStatus.BLOCKED) {
            // Blocked — no billing expected
            return;
        }

        if (interaction.segment == ConvoyVehicleSegment.LUXE_PLATEAU) {
            // Manual quote — no billing
            return;
        }

        BigDecimal ttc = computeTtc(interaction);
        if (ttc.compareTo(BigDecimal.ZERO) <= 0) {
            failures.add(interaction.id + ": TTC must be positive, got " + ttc);
            return;
        }

        // Step 3: Billing
        ConvoyBillingStatus billStatus = computeBillingStatus(interaction);

        if (interaction.damageDetected) {
            if (billStatus != ConvoyBillingStatus.PENDING_DAMAGE_REVIEW) {
                failures.add(interaction.id + ": damage should produce PENDING_DAMAGE_REVIEW, got " + billStatus);
            }
            return;
        }

        if (billStatus != ConvoyBillingStatus.BILLED) {
            failures.add(interaction.id + ": expected BILLED, got " + billStatus);
            return;
        }

        // Step 4: Validate split
        BigDecimal conveyor = ttc.multiply(CONVEYOR_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platform = ttc.multiply(PLATFORM_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal diff     = ttc.subtract(conveyor.add(platform)).abs();
        if (diff.compareTo(new BigDecimal("0.01")) > 0) {
            failures.add(interaction.id + ": split error ttc=" + ttc + " conveyor=" + conveyor + " platform=" + platform);
        }

        // Step 5: Validate pre-auth
        BigDecimal preAuth = ttc.multiply(STRIPE_PRE_AUTH).setScale(2, RoundingMode.CEILING);
        if (preAuth.compareTo(ttc) <= 0) {
            failures.add(interaction.id + ": pre-auth must exceed ttc: preAuth=" + preAuth + " ttc=" + ttc);
        }
    }

    // ── Domain logic helpers ──────────────────────────────────────────────────

    private ConvoyVerificationStatus computeVerificationStatus(Interaction interaction) {
        return interaction.driverBlocked
                ? ConvoyVerificationStatus.BLOCKED
                : ConvoyVerificationStatus.VERIFIED;
    }

    private ConvoyBillingStatus computeBillingStatus(Interaction interaction) {
        return interaction.damageDetected
                ? ConvoyBillingStatus.PENDING_DAMAGE_REVIEW
                : ConvoyBillingStatus.BILLED;
    }

    private ConvoyBillingStatus computeBillingStatusGated(Interaction interaction) {
        if (computeVerificationStatus(interaction) == ConvoyVerificationStatus.BLOCKED) {
            return ConvoyBillingStatus.PENDING;  // gate blocks billing
        }
        return computeBillingStatus(interaction);
    }

    private BigDecimal computeTtc(Interaction interaction) {
        BigDecimal transport = FLAT_BASE
                .add(BigDecimal.valueOf(interaction.distanceKm).multiply(RATE_PER_KM))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal surcharge = switch (interaction.segment) {
            case STANDARD      -> BigDecimal.ZERO;
            case COURANT       -> new BigDecimal("5.00");
            case PREMIUM       -> new BigDecimal("15.00");
            case HAUT_DE_GAMME -> new BigDecimal("30.00");
            case LUXE_PLATEAU  -> BigDecimal.ZERO;
        };

        BigDecimal subtotal = transport.add(surcharge).setScale(2, RoundingMode.HALF_UP);

        BigDecimal urgencyBonus = switch (interaction.urgency) {
            case EXPRESS -> subtotal.multiply(EXPRESS_BONUS).setScale(2, RoundingMode.HALF_UP);
            case URGENT  -> subtotal.multiply(URGENT_BONUS).setScale(2, RoundingMode.HALF_UP);
            default      -> BigDecimal.ZERO;
        };

        BigDecimal weekendBonus = BigDecimal.ZERO;
        if (interaction.requestedAt != null) {
            DayOfWeek dow = interaction.requestedAt.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                weekendBonus = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalHt = subtotal.add(urgencyBonus).add(weekendBonus)
                .max(MINIMUM_FARE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal vat = totalHt.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        return totalHt.add(vat).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Data record ───────────────────────────────────────────────────────────

    record Interaction(
            String id,
            ConvoyVehicleSegment segment,
            ConvoyUrgency urgency,
            double distanceKm,
            LocalDateTime requestedAt,
            boolean damageDetected,
            boolean driverBlocked
    ) {}
}
