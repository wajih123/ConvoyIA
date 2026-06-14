package com.goweyy.convoyia.inspector.agent;

import com.goweyy.convoyia.inspector.domain.InspectionPhase;
import com.goweyy.convoyia.inspector.domain.InspectionResult;
import org.springframework.stereotype.Component;

@Component
public class InspectorPromptBuilder {

    public String buildVisionPrompt(String base64Image, InspectionPhase phase) {
        return buildVisionPrompt(base64Image, phase, "french");
    }

    public String buildVisionPrompt(String base64Image, InspectionPhase phase, String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un expert en inspection automobile pour une plateforme de convoyage en France.
                Phase d'inspection: %s
                
                Analyse cette image du véhicule et fournis un rapport détaillé.
                
                Image (base64): [IMAGE_DATA: %s]
                
                %s
                {
                  "damageAreas": ["Zone 1", "Zone 2"],
                  "severity": "NONE|MINOR|MAJOR|CRITICAL",
                  "odometer": null,
                  "fuelLevel": null,
                  "overallCondition": "EXCELLENT|GOOD|FAIR|POOR",
                  "notes": "Observations détaillées"
                }
                """.formatted(
                phase.name(),
                base64Image.substring(0, Math.min(100, base64Image.length())) + "...",
                jsonInstruction
        );
    }

    public String buildComparisonPrompt(InspectionResult pre, InspectionResult post) {
        return buildComparisonPrompt(pre, post, "french");
    }

    public String buildComparisonPrompt(InspectionResult pre, InspectionResult post, String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un expert en évaluation de dommages automobiles pour une plateforme de convoyage en France.
                
                Compare les résultats d'inspection avant et après mission et identifie les nouveaux dommages.
                
                INSPECTION PRÉ-MISSION:
                - Sévérité: %s
                - Zones touchées: %s
                - Condition générale: %s
                
                INSPECTION POST-MISSION:
                - Sévérité: %s
                - Zones touchées: %s
                - Condition générale: %s
                
                %s
                {
                  "newDamages": ["Dommage 1", "Dommage 2"],
                  "verdict": "CLEAN|MINOR_DAMAGE|MAJOR_DAMAGE",
                  "recommendation": "Recommandation pour l'opérateur"
                }
                """.formatted(
                pre.getDamageReport() != null ? pre.getDamageReport().getSeverity() : "N/A",
                pre.getDamageReport() != null ? pre.getDamageReport().getAreas() : "[]",
                "N/A",
                post.getDamageReport() != null ? post.getDamageReport().getSeverity() : "N/A",
                post.getDamageReport() != null ? post.getDamageReport().getAreas() : "[]",
                "N/A",
                jsonInstruction
        );
    }

    /**
     * Resolves the JSON-only instruction string for the given language.
     */
    static String resolveJsonInstruction(String language) {
        return switch (language == null ? "french" : language.toLowerCase()) {
            case "french"  -> "Réponds UNIQUEMENT en JSON valide:";
            case "german"  -> "Antworte NUR mit gültigem JSON:";
            case "spanish" -> "Responde ÚNICAMENTE en JSON válido:";
            case "arabic"  -> "أجب فقط بـ JSON صالح:";
            default        -> "Reply ONLY with valid JSON:";
        };
    }
}
