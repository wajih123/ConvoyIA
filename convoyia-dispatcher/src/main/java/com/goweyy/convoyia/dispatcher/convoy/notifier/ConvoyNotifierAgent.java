package com.goweyy.convoyia.dispatcher.convoy.notifier;

import com.goweyy.convoyia.common.domain.enums.ConvoyNotificationTemplate;
import com.goweyy.convoyia.common.domain.tenant.ConvoyTenantConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvoyNotifierAgent {

    private final ConvoyTenantConfigRepository tenantConfigRepository;
    private final ConvoySmsService smsService;

    public void notify(String missionId, ConvoyNotificationTemplate template, Map<String, Object> data) {
        log.info("[ConvoyNotifier] missionId={} template={} data={}", missionId, template, data);
        // Core notification logic: log + optionally send SMS
        // TODO: integrate EmailService from Nevyo
        // TODO: integrate FcmPushService from Nevyo for push notifications
        String message = buildMessage(template, data);
        log.info("[ConvoyNotifier] Notification: {}", message);
        Object toNumber = data.get("phoneNumber");
        if (toNumber instanceof String phone && !phone.isBlank()) {
            smsService.sendSms(phone, message);
        }
    }

    private String buildMessage(ConvoyNotificationTemplate template, Map<String, Object> data) {
        return switch (template) {
            case MISSION_CONFIRMED -> "Mission confirmed. Reference: " + data.getOrDefault("missionId", "");
            case DRIVER_ASSIGNED -> "Driver assigned to your mission.";
            case DRIVER_EN_ROUTE -> "Your driver is en route.";
            case MISSION_STARTED -> "Mission started.";
            case MISSION_COMPLETED -> "Mission completed. Total: " + data.getOrDefault("totalTtc", "") + " " + data.getOrDefault("currency", "");
            case MISSION_CANCELLED -> "Mission cancelled. Refund: " + data.getOrDefault("refundAmount", "");
            case DAMAGE_DETECTED -> "Damage detected on vehicle. Claim in progress.";
            case PAYMENT_CONFIRMED -> "Payment confirmed.";
            case SURGE_CONFIRMATION_REQUIRED -> "New price: " + data.getOrDefault("newPrice", "") + ". Please confirm to proceed.";
            case DISPUTE_OPENED -> "Dispute opened for your mission.";
            case DISPUTE_RESOLVED -> "Dispute resolved: " + data.getOrDefault("resolution", "");
            case MISSION_UNAVAILABLE -> "No driver available: " + data.getOrDefault("reason", "");
            default -> template.name();
        };
    }
}
