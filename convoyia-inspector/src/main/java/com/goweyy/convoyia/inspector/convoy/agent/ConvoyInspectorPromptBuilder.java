package com.goweyy.convoyia.inspector.convoy.agent;

import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionRequest;
import org.springframework.stereotype.Component;

@Component
public class ConvoyInspectorPromptBuilder {

    public String buildInspectionPrompt(ConvoyInspectionRequest request) {
        return String.format("""
                You are a vehicle inspection AI agent.
                Analyze the provided photos for the vehicle (plate: %s).
                Phase: %s
                Return JSON: {
                  "damageDetected": true/false,
                  "damagedZones": ["front", "rear", ...],
                  "severity": "INFO|WARNING|CRITICAL",
                  "description": "<brief description>"
                }
                """,
                request.getVehiclePlate(), request.getPhase());
    }
}
