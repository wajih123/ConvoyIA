package com.goweyy.convoyia.dispatcher.convoy.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/convoy/schedule")
@RequiredArgsConstructor
public class ConvoySchedulerController {

    private final ConvoySchedulerAgent schedulerAgent;

    @PostMapping("/cancel")
    public ResponseEntity<ConvoyScheduleResult> cancel(@RequestBody ConvoyCancelRequest request) {
        return ResponseEntity.ok(schedulerAgent.cancel(request.getMissionId(), request.getRequestedBy(), request.getReason()));
    }

    @PostMapping("/reschedule")
    public ResponseEntity<ConvoyScheduleResult> reschedule(@RequestBody ConvoyRescheduleRequest request) {
        return ResponseEntity.ok(schedulerAgent.reschedule(request.getMissionId(), request.getNewDateTime(), request.getRequestedBy()));
    }
}
