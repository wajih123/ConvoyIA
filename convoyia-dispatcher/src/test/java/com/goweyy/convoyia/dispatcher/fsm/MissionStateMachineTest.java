package com.goweyy.convoyia.dispatcher.fsm;

import com.goweyy.convoyia.common.domain.enums.MissionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MissionStateMachineTest {

    private MissionStateMachine fsm;

    @BeforeEach
    void setUp() {
        fsm = new MissionStateMachine();
    }

    // ── Valid transitions ────────────────────────────────────────────────────

    @Test
    void valid_transition_returns_next_state() {
        MissionState result = fsm.transition(MissionState.RECEIVED, MissionState.QUALIFYING, "mission-001");
        assertThat(result).isEqualTo(MissionState.QUALIFYING);
    }

    @Test
    void full_happy_path_receives_to_completed() {
        String id = "mission-happy-path";
        MissionState state = MissionState.RECEIVED;
        state = fsm.transition(state, MissionState.QUALIFYING, id);
        state = fsm.transition(state, MissionState.ROUTING, id);
        state = fsm.transition(state, MissionState.PENDING_VERIFICATION, id);
        state = fsm.transition(state, MissionState.PENDING_PRICING, id);
        state = fsm.transition(state, MissionState.PENDING_ASSIGNMENT, id);
        state = fsm.transition(state, MissionState.IN_PROGRESS, id);
        state = fsm.transition(state, MissionState.COMPLETED, id);
        assertThat(state).isEqualTo(MissionState.COMPLETED);
    }

    @Test
    void escalation_from_routing_then_re_route() {
        String id = "mission-escalate-001";
        MissionState state = MissionState.ROUTING;
        state = fsm.transition(state, MissionState.ESCALATED_HUMAN, id);
        assertThat(state).isEqualTo(MissionState.ESCALATED_HUMAN);
        state = fsm.transition(state, MissionState.ROUTING, id);
        assertThat(state).isEqualTo(MissionState.ROUTING);
    }

    @Test
    void fail_from_in_progress() {
        String id = "mission-fail-001";
        MissionState state = MissionState.IN_PROGRESS;
        state = fsm.transition(state, MissionState.FAILED, id);
        assertThat(state).isEqualTo(MissionState.FAILED);
    }

    // ── Invalid transitions throw ────────────────────────────────────────────

    @Test
    void invalid_transition_throws_illegal_state_exception() {
        assertThatThrownBy(() ->
                fsm.transition(MissionState.RECEIVED, MissionState.IN_PROGRESS, "bad-mission"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid transition")
                .hasMessageContaining("RECEIVED")
                .hasMessageContaining("IN_PROGRESS");
    }

    @Test
    void transition_from_completed_always_throws() {
        for (MissionState target : MissionState.values()) {
            assertThatThrownBy(() ->
                    fsm.transition(MissionState.COMPLETED, target, "completed-mission"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void transition_from_failed_always_throws() {
        for (MissionState target : MissionState.values()) {
            assertThatThrownBy(() ->
                    fsm.transition(MissionState.FAILED, target, "failed-mission"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void backward_transition_throws() {
        assertThatThrownBy(() ->
                fsm.transition(MissionState.IN_PROGRESS, MissionState.RECEIVED, "backward-mission"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── fail() and escalate() helpers ───────────────────────────────────────

    @Test
    void fail_helper_returns_failed_state() {
        MissionState result = fsm.fail(MissionState.ROUTING, "mission-002", "driver unreachable");
        assertThat(result).isEqualTo(MissionState.FAILED);
    }

    @Test
    void escalate_helper_returns_escalated_human_state() {
        MissionState result = fsm.escalate(MissionState.QUALIFYING, "mission-003", 0.42);
        assertThat(result).isEqualTo(MissionState.ESCALATED_HUMAN);
    }

    @Test
    void escalate_with_zero_confidence() {
        MissionState result = fsm.escalate(MissionState.PENDING_VERIFICATION, "mission-004", 0.0);
        assertThat(result).isEqualTo(MissionState.ESCALATED_HUMAN);
    }
}
