package com.goweyy.convoyia.common.repository;

import com.goweyy.convoyia.common.domain.enums.ConvoyMissionState;
import com.goweyy.convoyia.common.domain.enums.ConvoyMissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConvoyMissionContextRepository extends JpaRepository<ConvoyMissionContext, UUID> {

    Optional<ConvoyMissionContext> findByMissionId(UUID missionId);

    List<ConvoyMissionContext> findByTenantId(String tenantId);

    List<ConvoyMissionContext> findAllByTenantId(String tenantId);

    List<ConvoyMissionContext> findAllByTenantIdAndCurrentState(String tenantId, ConvoyMissionState currentState);

    List<ConvoyMissionContext> findAllByTenantIdAndMissionType(String tenantId, ConvoyMissionType missionType);
}
