package com.goweyy.convoyia.verifier.llm;

import com.goweyy.convoyia.common.domain.records.VehicleData;
import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import org.springframework.stereotype.Component;

@Component
public class VerifierPromptBuilder {

    private static final String SEGMENT_RANGES = """
            - STANDARD: véhicules courants, valeur < 15 000 EUR
            - COURANT: 15 000 - 30 000 EUR
            - PREMIUM: 30 000 - 60 000 EUR
            - HAUT_DE_GAMME: 60 000 - 120 000 EUR
            - LUXE_PLATEAU: > 120 000 EUR
            """;

    public String buildVehicleCoherencePrompt(VehicleData vehicle) {
        return buildVehicleCoherencePrompt(vehicle, "french");
    }

    public String buildVehicleCoherencePrompt(VehicleData vehicle, String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un expert en évaluation automobile pour une plateforme de convoyage en France.
                
                Analyse la cohérence entre les données du véhicule et le segment déclaré.
                
                Données véhicule:
                - Marque: %s
                - Modèle: %s
                - Année: %d
                - Valeur déclarée: %.2f EUR
                - Segment déclaré: %s
                
                Segments et plages:
                %s
                
                %s
                {
                  "coherent": true,
                  "expectedSegment": "SEGMENT_VALUE",
                  "confidence": 0.9,
                  "reason": "Explication"
                }
                """.formatted(
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getDeclaredValue(),
                vehicle.getDeclaredSegment(),
                SEGMENT_RANGES,
                jsonInstruction
        );
    }

    public String buildAddressValidationPrompt(String origin, String destination) {
        return buildAddressValidationPrompt(origin, destination, "french");
    }

    public String buildAddressValidationPrompt(String origin, String destination, String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un expert en géolocalisation pour la France.
                
                Valide que les adresses suivantes sont des adresses françaises valides:
                - Adresse d'origine: %s
                - Adresse de destination: %s
                
                %s
                {
                  "valid": true,
                  "originValid": true,
                  "destinationValid": true,
                  "reason": "Explication"
                }
                """.formatted(origin, destination, jsonInstruction);
    }

    public String buildEscalationPrompt(VerificationRequest req,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock vehicle,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock conveyor,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock mission) {
        return buildEscalationPrompt(req, vehicle, conveyor, mission, "french");
    }

    public String buildEscalationPrompt(VerificationRequest req,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock vehicle,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock conveyor,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock mission,
                                         String language) {
        String jsonInstruction = resolveJsonInstruction(language);
        return """
                Tu es un agent de supervision pour une plateforme de convoyage automobile en France.
                
                Une vérification a nécessité une escalade. Analyse les résultats et donne une recommandation.
                
                Mission ID: %s
                Tenant: %s
                
                Résultat vérification véhicule: %s
                Résultat vérification convoyeur: %s
                Résultat vérification mission: %s
                
                %s
                {
                  "recommendation": "APPROVE|REJECT|REVIEW",
                  "priorityLevel": "LOW|MEDIUM|HIGH|CRITICAL",
                  "summary": "Résumé pour l'opérateur humain",
                  "requiredActions": ["Action 1", "Action 2"]
                }
                """.formatted(
                req.getMissionId(),
                req.getTenantId(),
                vehicle != null ? vehicle.getStatus() : "N/A",
                conveyor != null ? conveyor.getStatus() : "N/A",
                mission != null ? mission.getStatus() : "N/A",
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
