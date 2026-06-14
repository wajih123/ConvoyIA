package com.goweyy.convoyia.dispatcher.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goweyy.convoyia.common.domain.records.MissionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MissionContextRepository {

    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    public Mono<Void> saveContext(MissionContext context) {
        try {
            String agentTrace = objectMapper.writeValueAsString(context.getAgentTrace());
            String enrichedData = objectMapper.writeValueAsString(context.getEnrichedData());

            return databaseClient.sql("""
                            INSERT INTO mission_contexts (mission_id, tenant_id, current_state, vehicle_segment,
                                confidence_score, agent_trace, enriched_data, last_updated)
                            VALUES (:missionId, :tenantId, :currentState, :vehicleSegment,
                                :confidenceScore, :agentTrace::text, :enrichedData::text, :lastUpdated)
                            ON CONFLICT (mission_id) DO UPDATE SET
                                current_state = EXCLUDED.current_state,
                                vehicle_segment = EXCLUDED.vehicle_segment,
                                confidence_score = EXCLUDED.confidence_score,
                                agent_trace = EXCLUDED.agent_trace,
                                enriched_data = EXCLUDED.enriched_data,
                                last_updated = EXCLUDED.last_updated
                            """)
                    .bind("missionId", UUID.fromString(context.getMissionId()))
                    .bind("tenantId", context.getTenantId())
                    .bind("currentState", context.getCurrentState().name())
                    .bind("vehicleSegment", context.getVehicleSegment() != null ? context.getVehicleSegment().name() : null)
                    .bind("confidenceScore", context.getConfidenceScore() != null ? context.getConfidenceScore() : null)
                    .bind("agentTrace", agentTrace)
                    .bind("enrichedData", enrichedData)
                    .bind("lastUpdated", context.getLastUpdated() != null ? context.getLastUpdated() : Instant.now())
                    .fetch()
                    .rowsUpdated()
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize mission context", e));
        }
    }

    public Mono<MissionContext> findByMissionId(String missionId) {
        return databaseClient.sql("SELECT * FROM mission_contexts WHERE mission_id = :missionId")
                .bind("missionId", UUID.fromString(missionId))
                .fetch()
                .one()
                .map(row -> {
                    try {
                        return MissionContext.builder()
                                .missionId(row.get("mission_id").toString())
                                .tenantId((String) row.get("tenant_id"))
                                .currentState(com.goweyy.convoyia.common.domain.enums.MissionState
                                        .valueOf((String) row.get("current_state")))
                                .vehicleSegment(row.get("vehicle_segment") != null
                                        ? com.goweyy.convoyia.common.domain.enums.VehicleSegment
                                        .valueOf((String) row.get("vehicle_segment"))
                                        : null)
                                .confidenceScore(row.get("confidence_score") != null
                                        ? ((Number) row.get("confidence_score")).doubleValue()
                                        : null)
                                .agentTrace(objectMapper.readValue(
                                        (String) row.get("agent_trace"),
                                        objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)))
                                .lastUpdated((Instant) row.get("last_updated"))
                                .build();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize mission context", e);
                    }
                });
    }
}
