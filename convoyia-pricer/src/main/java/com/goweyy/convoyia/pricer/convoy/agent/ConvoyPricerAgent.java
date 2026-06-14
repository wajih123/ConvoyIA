package com.goweyy.convoyia.pricer.convoy.agent;

import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingRequest;
import com.goweyy.convoyia.pricer.convoy.dto.ConvoyPricingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyPricerAgent {

    private final ConvoyPricingCalculationService calculationService;

    public ConvoyPricingResult price(ConvoyPricingRequest request) {
        log.info("ConvoyPricerAgent pricing missionId={}", request.getMissionId());
        return calculationService.calculate(request);
    }
}
