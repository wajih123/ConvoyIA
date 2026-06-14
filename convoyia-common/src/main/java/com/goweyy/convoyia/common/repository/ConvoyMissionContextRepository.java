package com.goweyy.convoyia.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConvoyMissionContextRepository extends JpaRepository<ConvoyMissionContext, UUID> {

    Optional<ConvoyMissionContext> findByMissionId(UUID missionId);

    java.util.List<ConvoyMissionContext> findByTenantId(String tenantId);
}
