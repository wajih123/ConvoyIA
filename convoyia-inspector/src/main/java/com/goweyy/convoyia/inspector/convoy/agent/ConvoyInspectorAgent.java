package com.goweyy.convoyia.inspector.convoy.agent;

import com.goweyy.convoyia.common.domain.enums.ConvoyAlertSeverity;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyDamageReport;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionRequest;
import com.goweyy.convoyia.inspector.convoy.dto.ConvoyInspectionResult;
import com.goweyy.convoyia.inspector.convoy.vision.ConvoyQwenVlAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyInspectorAgent {

    private final ConvoyInspectorPromptBuilder promptBuilder;
    private final ConvoyQwenVlAdapter qwenVlAdapter;

    public ConvoyInspectionResult inspect(ConvoyInspectionRequest request) {
        log.info("ConvoyInspectorAgent inspecting missionId={} phase={}", request.getMissionId(), request.getPhase());

        String analysisResult = "";
        if (request.getPhotoUrls() != null && !request.getPhotoUrls().isEmpty()) {
            String prompt = promptBuilder.buildInspectionPrompt(request);
            analysisResult = qwenVlAdapter.analyze(prompt, request.getPhotoUrls());
        }

        boolean damageDetected = analysisResult.toLowerCase().contains("damage")
                || analysisResult.toLowerCase().contains("scratch")
                || analysisResult.toLowerCase().contains("dent");

        ConvoyDamageReport damageReport = ConvoyDamageReport.builder()
                .damageDetected(damageDetected)
                .damagedZones(Collections.emptyList())
                .severity(damageDetected ? ConvoyAlertSeverity.WARNING : ConvoyAlertSeverity.INFO)
                .description(analysisResult.isEmpty() ? "No photos provided" : analysisResult)
                .build();

        return ConvoyInspectionResult.builder()
                .missionId(request.getMissionId())
                .tenantId(request.getTenantId())
                .phase(request.getPhase())
                .passed(!damageDetected)
                .damageReport(damageReport)
                .notes(analysisResult)
                .inspectedAt(Instant.now())
                .build();
    }
}
