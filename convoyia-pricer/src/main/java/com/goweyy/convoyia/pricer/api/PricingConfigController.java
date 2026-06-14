package com.goweyy.convoyia.pricer.api;

import com.goweyy.convoyia.pricer.domain.PricingFormulaConfig;
import com.goweyy.convoyia.pricer.domain.PricingRequest;
import com.goweyy.convoyia.pricer.domain.PricingResult;
import com.goweyy.convoyia.pricer.repository.TenantPricingConfigRepository;
import com.goweyy.convoyia.pricer.service.PricingCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Admin API for managing tenant pricing configurations.
 * Secured by Keycloak role TENANT_ADMIN.
 * tenantId is always extracted from JWT/header — never from request body.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
public class PricingConfigController {

    private final TenantPricingConfigRepository configRepository;
    private final PricingCalculationService calculationService;

    /**
     * GET current tenant pricing configuration.
     * tenantId extracted from X-Tenant-Id header (set by gateway from JWT claim).
     */
    @GetMapping("/config")
    public Mono<ResponseEntity<PricingFormulaConfig>> getConfig(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return configRepository.findActiveByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * PUT update tenant pricing configuration.
     * White-label tenants use this to override Goweyy defaults.
     * tenantId extracted from X-Tenant-Id header — body tenantId is ignored.
     */
    @PutMapping("/config")
    public Mono<ResponseEntity<Void>> updateConfig(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody PricingFormulaConfig config) {
        // Enforce tenantId from header — never trust body
        PricingFormulaConfig securedConfig = PricingFormulaConfig.builder()
                .tenantId(tenantId)
                .transportMode(config.getTransportMode())
                .ratePerKm(config.getRatePerKm())
                .flatBaseFare(config.getFlatBaseFare())
                .minimumFare(config.getMinimumFare())
                .platformFeeRatio(config.getPlatformFeeRatio())
                .vatRate(config.getVatRate())
                .stripePreAuthMultiplier(config.getStripePreAuthMultiplier())
                .insuranceConfig(config.getInsuranceConfig())
                .segmentSurcharges(config.getSegmentSurcharges())
                .contextMultipliers(config.getContextMultipliers())
                .active(config.isActive())
                .build();

        return configRepository.upsertConfig(securedConfig)
                .thenReturn(ResponseEntity.<Void>ok().build())
                .doOnSuccess(r -> log.info("Pricing config updated for tenantId={}", tenantId));
    }

    /**
     * POST dry-run price simulation with given parameters.
     * Returns a full PricingResult without publishing any Kafka events.
     * tenantId extracted from X-Tenant-Id header.
     */
    @PostMapping("/simulate")
    public Mono<ResponseEntity<PricingResult>> simulate(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody PricingRequest request) {
        // Override tenantId from header
        PricingRequest securedRequest = PricingRequest.builder()
                .missionId(request.getMissionId() != null ? request.getMissionId() : "simulation")
                .tenantId(tenantId)
                .vehicleSegment(request.getVehicleSegment())
                .vehicleDeclaredValue(request.getVehicleDeclaredValue())
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .requestedAt(request.getRequestedAt())
                .urgency(request.getUrgency())
                .build();

        return calculationService.calculate(securedRequest)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Pricing simulation failed for tenantId={}: {}", tenantId, e.getMessage()));
    }
}
