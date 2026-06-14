package com.goweyy.convoyia.dispatcher.fsm;

import com.goweyy.convoyia.common.domain.enums.MissionState;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class MissionTransitions {

    private static final Map<MissionState, Set<MissionState>> TRANSITIONS = new EnumMap<>(MissionState.class);

    static {
        TRANSITIONS.put(MissionState.RECEIVED, EnumSet.of(MissionState.QUALIFYING, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.QUALIFYING, EnumSet.of(MissionState.ROUTING, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.ROUTING, EnumSet.of(MissionState.PENDING_VERIFICATION, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.PENDING_VERIFICATION, EnumSet.of(MissionState.PENDING_PRICING, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.PENDING_PRICING, EnumSet.of(MissionState.PENDING_ASSIGNMENT, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.PENDING_ASSIGNMENT, EnumSet.of(MissionState.IN_PROGRESS, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.IN_PROGRESS, EnumSet.of(MissionState.COMPLETED, MissionState.FAILED, MissionState.ESCALATED_HUMAN));
        TRANSITIONS.put(MissionState.COMPLETED, EnumSet.noneOf(MissionState.class));
        TRANSITIONS.put(MissionState.FAILED, EnumSet.noneOf(MissionState.class));
        TRANSITIONS.put(MissionState.ESCALATED_HUMAN, EnumSet.of(MissionState.ROUTING, MissionState.FAILED));
    }

    public static boolean isValidTransition(MissionState from, MissionState to) {
        Set<MissionState> allowed = TRANSITIONS.getOrDefault(from, EnumSet.noneOf(MissionState.class));
        return allowed.contains(to);
    }

    public static Set<MissionState> getAllowedTransitions(MissionState from) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(MissionState.class));
    }
}
