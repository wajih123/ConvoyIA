package com.goweyy.convoyia.dispatcher.fsm;

import com.goweyy.convoyia.common.domain.enums.MissionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MissionStateMachine {

    public MissionState transition(MissionState current, MissionState next, String missionId) {
        if (MissionTransitions.isValidTransition(current, next)) {
            log.info("Mission {} state transition: {} -> {}", missionId, current, next);
            return next;
        } else {
            log.warn("Invalid state transition for mission {}: {} -> {}", missionId, current, next);
            throw new IllegalStateException(
                    "Invalid transition from " + current + " to " + next + " for mission " + missionId);
        }
    }

    public MissionState fail(MissionState current, String missionId, String reason) {
        log.error("Mission {} failed from state {}: {}", missionId, current, reason);
        return MissionState.FAILED;
    }

    public MissionState escalate(MissionState current, String missionId, double confidence) {
        log.info("Mission {} escalated to human from state {} (confidence={})", missionId, current, confidence);
        return MissionState.ESCALATED_HUMAN;
    }
}
