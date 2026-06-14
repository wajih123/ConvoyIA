package com.goweyy.convoyia.dispatcher.agent;

import com.goweyy.convoyia.common.domain.enums.LlmModel;
import com.goweyy.convoyia.common.domain.enums.ResponseFormat;
import com.goweyy.convoyia.common.domain.enums.VehicleSegment;
import com.goweyy.convoyia.common.domain.records.MissionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatcherPromptBuilder {

    private static final String SEGMENT_RANGES = """
            - STANDARD: véhicules courants, valeur déclarée < 15 000 EUR
            - COURANT: véhicules récents standard, 15 000 - 30 000 EUR
            - PREMIUM: véhicules haut de gamme standards, 30 000 - 60 000 EUR
            - HAUT_DE_GAMME: véhicules de luxe, 60 000 - 120 000 EUR
            - LUXE_PLATEAU: véhicules très haut de gamme (Ferrari, Lamborghini, Rolls-Royce, etc.), > 120 000 EUR
            """;

    public String buildQualificationPrompt(MissionRequest request) {
        return buildQualificationPrompt(request, "french");
    }

    public String buildQualificationPrompt(MissionRequest request, String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un agent de qualification de mission pour une plateforme de convoyage automobile en France.
                
                Analyse la demande de mission suivante et détermine le segment du véhicule et le niveau d'urgence.
                
                Demande:
                - Client ID: %s
                - Véhicule ID: %s
                - Adresse d'origine: %s
                - Adresse de destination: %s
                - Date/Heure demandée: %s
                - Urgence déclarée: %s
                - Métadonnées: %s
                
                Segments disponibles et leurs plages tarifaires:
                %s
                
                %s
                {
                  "segment": "SEGMENT_VALUE",
                  "confidence": 0.85,
                  "urgencyConfirmed": "STANDARD|EXPRESS|URGENT",
                  "qualificationNotes": "Explication courte"
                }
                """.formatted(
                request.getClientId(),
                request.getVehicleId(),
                request.getOriginAddress(),
                request.getDestinationAddress(),
                request.getRequestedAt(),
                request.getUrgency(),
                request.getMetadata(),
                SEGMENT_RANGES,
                jsonInstruction
        );
    }

    public String buildRoutingPrompt(com.goweyy.convoyia.common.domain.records.MissionContext context) {
        return buildRoutingPrompt(context, "french");
    }

    public String buildRoutingPrompt(com.goweyy.convoyia.common.domain.records.MissionContext context, String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un agent de routage pour une plateforme de convoyage automobile en France.
                
                Planifie le routage optimal pour cette mission de convoyage.
                
                Mission ID: %s
                Segment: %s
                Urgence: %s
                Origine: %s
                Destination: %s
                Confiance qualification: %.2f
                
                %s
                {
                  "estimatedDurationMin": 120,
                  "preferredTimeSlot": "MATIN|APRES_MIDI|SOIR",
                  "returnMode": "BOLT_BUSINESS|UBER|TRAIN|AUTRE",
                  "notes": "Notes de routage",
                  "summary": "Résumé de la mission"
                }
                """.formatted(
                context.getMissionId(),
                context.getVehicleSegment(),
                context.getOriginalRequest() != null ? context.getOriginalRequest().getUrgency() : "N/A",
                context.getOriginalRequest() != null ? context.getOriginalRequest().getOriginAddress() : "N/A",
                context.getOriginalRequest() != null ? context.getOriginalRequest().getDestinationAddress() : "N/A",
                context.getConfidenceScore() != null ? context.getConfidenceScore() : 0.0,
                jsonInstruction
        );
    }

    /**
     * Resolves the JSON-only instruction string for the given language.
     */
    static String resolveJsonInstruction(String language) {
        return switch (language == null ? "french" : language.toLowerCase()) {
            case "french"  -> "Réponds UNIQUEMENT en JSON valide avec ce format:";
            case "german"  -> "Antworte NUR mit gültigem JSON in diesem Format:";
            case "spanish" -> "Responde ÚNICAMENTE en JSON válido con este formato:";
            case "arabic"  -> "أجب فقط بـ JSON صالح بهذا الشكل:";
            default        -> "Reply ONLY with valid JSON in this format:";
        };
    }
}
