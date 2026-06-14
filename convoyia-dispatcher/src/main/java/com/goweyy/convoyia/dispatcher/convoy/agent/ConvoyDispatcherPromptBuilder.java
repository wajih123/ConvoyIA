package com.goweyy.convoyia.dispatcher.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyVehicleSegment;
import com.goweyy.convoyia.dispatcher.convoy.dto.ConvoyDispatchRequest;
import org.springframework.stereotype.Component;

@Component
public class ConvoyDispatcherPromptBuilder {

    public String buildQualificationPrompt(ConvoyDispatchRequest request) {
        return String.format("""
                You are a vehicle conveyance qualification agent.
                Analyze this mission request and return a JSON object:
                {
                  "segment": "<STANDARD|COURANT|PREMIUM|HAUT_DE_GAMME|LUXE_PLATEAU>",
                  "confidence": <0.0-1.0>,
                  "urgencyConfirmed": "<STANDARD|EXPRESS|URGENT>"
                }
                Mission:
                - Origin: %s
                - Destination: %s
                - Vehicle declared value: %.0f EUR
                - Urgency: %s
                - Client aboard: %s
                """,
                request.getOriginAddress(),
                request.getDestinationAddress(),
                request.getVehicleDeclaredValue(),
                request.getUrgency(),
                request.isClientAboard());
    }

    public String buildRoutingPrompt(ConvoyDispatchRequest request, ConvoyVehicleSegment segment) {
        return String.format("""
                You are a routing agent for vehicle conveyance missions.
                Return a JSON object:
                {
                  "estimatedDurationMin": <integer>,
                  "notes": "<routing notes>",
                  "returnMode": "<BOLT|UBER|TRAIN|SELF>"
                }
                Mission:
                - Origin: %s
                - Destination: %s
                - Segment: %s
                """,
                request.getOriginAddress(),
                request.getDestinationAddress(),
                segment);
    }
}
