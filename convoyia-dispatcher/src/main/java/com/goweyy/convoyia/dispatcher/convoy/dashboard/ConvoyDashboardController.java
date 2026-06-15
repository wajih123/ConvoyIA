package com.goweyy.convoyia.dispatcher.convoy.dashboard;

import com.goweyy.convoyia.common.domain.enums.ConvoyMarket;
import com.goweyy.convoyia.common.repository.ConvoyMissionContext;
import com.goweyy.convoyia.common.repository.ConvoyMissionContextRepository;
import com.goweyy.convoyia.dispatcher.convoy.dashboard.dto.ConvoyMissionSummary;
import com.goweyy.convoyia.dispatcher.convoy.dashboard.dto.ConvoyTenantStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/convoy/dashboard")
@RequiredArgsConstructor
public class ConvoyDashboardController {

    private final ConvoyMissionContextRepository missionContextRepository;

    @GetMapping("/stats")
    public ResponseEntity<ConvoyTenantStats> getStats(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<ConvoyMissionContext> missions = missionContextRepository.findByTenantId(tenantId);

        Map<String, Long> byState = missions.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCurrentState() != null ? m.getCurrentState().name() : "UNKNOWN",
                        Collectors.counting()));

        ConvoyTenantStats stats = ConvoyTenantStats.builder()
                .tenantId(tenantId)
                .totalMissions(missions.size())
                .completedMissions(byState.getOrDefault("COMPLETED", 0L))
                .failedMissions(byState.getOrDefault("FAILED", 0L))
                .escalatedMissions(byState.getOrDefault("ESCALATED_HUMAN", 0L))
                .totalRevenueTtc(BigDecimal.ZERO) // TODO: sum from billing records
                .currencyCode(ConvoyMarket.FRANCE.getCurrencyCode())
                .missionsByState(byState)
                .build();

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/missions")
    public ResponseEntity<List<ConvoyMissionSummary>> getMissions(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<ConvoyMissionSummary> summaries = missionContextRepository.findByTenantId(tenantId)
                .stream()
                .map(m -> ConvoyMissionSummary.builder()
                        .missionId(m.getMissionId() != null ? m.getMissionId().toString() : null)
                        .tenantId(m.getTenantId())
                        .state(m.getCurrentState())
                        .originAddress(m.getOriginAddress())
                        .destinationAddress(m.getDestinationAddress())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }
}
