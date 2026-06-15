package com.goweyy.convoyia.dispatcher.convoy.booking;

import com.goweyy.convoyia.common.domain.enums.ConvoyNotificationTemplate;
import com.goweyy.convoyia.common.domain.enums.ConvoyReturnMode;
import com.goweyy.convoyia.dispatcher.convoy.notifier.ConvoyNotifierAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyReturnBookingService {

    private static final Pattern COORDINATE_PATTERN = Pattern.compile("(-?\\d+(?:\\.\\d+)?),\\s*(-?\\d+(?:\\.\\d+)?)");

    private final ConvoyNotifierAgent notifierAgent;

    public ConvoyReturnBookingResult bookReturn(String missionId, String arrivalAddress, String conveyorHomeCity) {
        double[] origin = extractCoordinates(arrivalAddress);
        double[] destination = extractCoordinates(conveyorHomeCity);
        double distanceKm = haversine(origin[0], origin[1], destination[0], destination[1]);
        int hour = LocalTime.now().getHour();
        ConvoyReturnMode mode = distanceKm < 50 ? ConvoyReturnMode.BOLT : distanceKm <= 300 ? ConvoyReturnMode.SNCF : ConvoyReturnMode.FLIGHT;
        if (mode != ConvoyReturnMode.BOLT && (hour >= 21 || hour < 6)) {
            mode = ConvoyReturnMode.HOTEL_PLUS_TRAIN;
        }
        BigDecimal estimatedCost = BigDecimal.ZERO;
        log.info("TODO: {} API not yet contracted", mode.name());
        notifierAgent.notify(missionId, ConvoyNotificationTemplate.RETURN_BOOKED, Map.of(
                "mode", mode,
                "estimatedCost", estimatedCost));
        return ConvoyReturnBookingResult.builder()
                .missionId(missionId)
                .mode(mode)
                .originAddress(arrivalAddress)
                .destinationAddress(conveyorHomeCity)
                .distanceKm(distanceKm)
                .estimatedCost(estimatedCost)
                .status("BOOKED")
                .build();
    }

    private double[] extractCoordinates(String input) {
        if (input == null) {
            return new double[] {0.0d, 0.0d};
        }
        Matcher matcher = COORDINATE_PATTERN.matcher(input);
        if (matcher.find()) {
            return new double[] {Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2))};
        }
        log.info("TODO: geocode address/city '{}' before provider integration", input);
        return new double[] {0.0d, 0.0d};
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
