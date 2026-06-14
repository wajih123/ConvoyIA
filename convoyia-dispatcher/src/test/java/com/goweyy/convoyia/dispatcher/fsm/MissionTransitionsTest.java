package com.goweyy.convoyia.dispatcher.fsm;

import com.goweyy.convoyia.common.domain.enums.MissionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MissionTransitionsTest {

    // ── Valid forward transitions ────────────────────────────────────────────

    @Test
    void received_can_transition_to_qualifying() {
        assertThat(MissionTransitions.isValidTransition(MissionState.RECEIVED, MissionState.QUALIFYING)).isTrue();
    }

    @Test
    void received_can_fail() {
        assertThat(MissionTransitions.isValidTransition(MissionState.RECEIVED, MissionState.FAILED)).isTrue();
    }

    @Test
    void received_can_escalate() {
        assertThat(MissionTransitions.isValidTransition(MissionState.RECEIVED, MissionState.ESCALATED_HUMAN)).isTrue();
    }

    @Test
    void qualifying_to_routing() {
        assertThat(MissionTransitions.isValidTransition(MissionState.QUALIFYING, MissionState.ROUTING)).isTrue();
    }

    @Test
    void routing_to_pending_verification() {
        assertThat(MissionTransitions.isValidTransition(MissionState.ROUTING, MissionState.PENDING_VERIFICATION)).isTrue();
    }

    @Test
    void pending_verification_to_pending_pricing() {
        assertThat(MissionTransitions.isValidTransition(MissionState.PENDING_VERIFICATION, MissionState.PENDING_PRICING)).isTrue();
    }

    @Test
    void pending_pricing_to_pending_assignment() {
        assertThat(MissionTransitions.isValidTransition(MissionState.PENDING_PRICING, MissionState.PENDING_ASSIGNMENT)).isTrue();
    }

    @Test
    void pending_assignment_to_in_progress() {
        assertThat(MissionTransitions.isValidTransition(MissionState.PENDING_ASSIGNMENT, MissionState.IN_PROGRESS)).isTrue();
    }

    @Test
    void in_progress_to_completed() {
        assertThat(MissionTransitions.isValidTransition(MissionState.IN_PROGRESS, MissionState.COMPLETED)).isTrue();
    }

    @Test
    void in_progress_can_fail() {
        assertThat(MissionTransitions.isValidTransition(MissionState.IN_PROGRESS, MissionState.FAILED)).isTrue();
    }

    @Test
    void escalated_human_can_route_again() {
        assertThat(MissionTransitions.isValidTransition(MissionState.ESCALATED_HUMAN, MissionState.ROUTING)).isTrue();
    }

    @Test
    void escalated_human_can_fail() {
        assertThat(MissionTransitions.isValidTransition(MissionState.ESCALATED_HUMAN, MissionState.FAILED)).isTrue();
    }

    // ── Terminal states have no transitions ─────────────────────────────────

    @Test
    void completed_is_terminal_no_transitions() {
        for (MissionState target : MissionState.values()) {
            assertThat(MissionTransitions.isValidTransition(MissionState.COMPLETED, target))
                    .as("COMPLETED → %s should be invalid", target)
                    .isFalse();
        }
    }

    @Test
    void failed_is_terminal_no_transitions() {
        for (MissionState target : MissionState.values()) {
            assertThat(MissionTransitions.isValidTransition(MissionState.FAILED, target))
                    .as("FAILED → %s should be invalid", target)
                    .isFalse();
        }
    }

    // ── Invalid backward / skip transitions ─────────────────────────────────

    @Test
    void cannot_go_from_in_progress_to_received() {
        assertThat(MissionTransitions.isValidTransition(MissionState.IN_PROGRESS, MissionState.RECEIVED)).isFalse();
    }

    @Test
    void cannot_skip_from_received_to_in_progress() {
        assertThat(MissionTransitions.isValidTransition(MissionState.RECEIVED, MissionState.IN_PROGRESS)).isFalse();
    }

    @Test
    void cannot_go_from_completed_to_in_progress() {
        assertThat(MissionTransitions.isValidTransition(MissionState.COMPLETED, MissionState.IN_PROGRESS)).isFalse();
    }

    @Test
    void cannot_go_from_pending_pricing_to_received() {
        assertThat(MissionTransitions.isValidTransition(MissionState.PENDING_PRICING, MissionState.RECEIVED)).isFalse();
    }

    // ── getAllowedTransitions returns correct sets ────────────────────────────

    @Test
    void received_allowed_set_contains_qualifying_failed_escalated() {
        Set<MissionState> allowed = MissionTransitions.getAllowedTransitions(MissionState.RECEIVED);
        assertThat(allowed).containsExactlyInAnyOrder(
                MissionState.QUALIFYING, MissionState.FAILED, MissionState.ESCALATED_HUMAN);
    }

    @Test
    void completed_allowed_set_is_empty() {
        assertThat(MissionTransitions.getAllowedTransitions(MissionState.COMPLETED)).isEmpty();
    }

    @Test
    void failed_allowed_set_is_empty() {
        assertThat(MissionTransitions.getAllowedTransitions(MissionState.FAILED)).isEmpty();
    }
}
