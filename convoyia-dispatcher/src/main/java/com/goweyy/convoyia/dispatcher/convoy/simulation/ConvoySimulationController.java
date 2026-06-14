package com.goweyy.convoyia.dispatcher.convoy.simulation;

import com.goweyy.convoyia.common.simulation.ConvoySimulationReport;
import com.goweyy.convoyia.common.simulation.ConvoySimulationRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/convoy/simulation")
@RequiredArgsConstructor
public class ConvoySimulationController {

    private final ConvoySimulationRunner simulationRunner;

    @PostMapping("/run")
    public ResponseEntity<ConvoySimulationReport> runAll() {
        return ResponseEntity.ok(simulationRunner.runAll());
    }
}
