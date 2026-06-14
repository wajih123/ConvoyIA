package com.goweyy.convoyia.verifier.llm;

import com.goweyy.convoyia.common.domain.records.ConveyorData;
import com.goweyy.convoyia.common.domain.records.VehicleData;
import com.goweyy.convoyia.common.domain.records.VerificationRequest;
import com.goweyy.convoyia.common.domain.records.VerificationResult;
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
                
                Réponds UNIQUEMENT en JSON valide:
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
                SEGMENT_RANGES
        );
    }

    public String buildAddressValidationPrompt(String origin, String destination) {
        return """
                Tu es un expert en géolocalisation pour la France.
                
                Valide que les adresses suivantes sont des adresses françaises valides:
                - Adresse d'origine: %s
                - Adresse de destination: %s
                
                Réponds UNIQUEMENT en JSON valide:
                {
                  "valid": true,
                  "originValid": true,
                  "destinationValid": true,
                  "reason": "Explication"
                }
                """.formatted(origin, destination);
    }

    public String buildEscalationPrompt(VerificationRequest req, 
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock vehicle,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock conveyor,
                                         com.goweyy.convoyia.common.domain.records.VerificationBlock mission) {
        return """
                Tu es un agent de supervision pour une plateforme de convoyage automobile en France.
                
                Une vérification a nécessité une escalade. Analyse les résultats et donne une recommandation.
                
                Mission ID: %s
                Tenant: %s
                
                Résultat vérification véhicule: %s
                Résultat vérification convoyeur: %s
                Résultat vérification mission: %s
                
                Réponds UNIQUEMENT en JSON valide:
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
                mission != null ? mission.getStatus() : "N/A"
        );
    }
}
