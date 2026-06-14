package com.goweyy.convoyia.verifier.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyVerificationStatus;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationBlock;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationRequest;
import com.goweyy.convoyia.verifier.convoy.dto.ConvoyVerificationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyVerifierAgent {

    private final ConvoyDriverVerifier driverVerifier;
    private final ConvoyVehicleVerifier vehicleVerifier;
    private final ConvoyMissionVerifier missionVerifier;

    public ConvoyVerificationResult verify(ConvoyVerificationRequest request) {
        log.info("ConvoyVerifierAgent verifying missionId={}", request.getMissionId());

        ConvoyVerificationBlock driverBlock = driverVerifier.verify(request);
        ConvoyVerificationBlock vehicleBlock = vehicleVerifier.verify(request);
        ConvoyVerificationBlock missionBlock = missionVerifier.verify(request);

        List<ConvoyVerificationBlock> blocks = List.of(driverBlock, vehicleBlock, missionBlock);
        boolean allPassed = blocks.stream().allMatch(ConvoyVerificationBlock::isPassed);
        boolean anyBlocked = blocks.stream().anyMatch(b -> !b.isPassed() && !b.getAlerts().isEmpty());

        ConvoyVerificationStatus status;
        if (allPassed) {
            status = ConvoyVerificationStatus.VERIFIED;
        } else if (anyBlocked) {
            status = ConvoyVerificationStatus.BLOCKED;
        } else {
            status = ConvoyVerificationStatus.PARTIAL;
        }

        return ConvoyVerificationResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .status(status)
                .blocks(blocks)
                .verifiedAt(Instant.now())
                .build();
    }
}
