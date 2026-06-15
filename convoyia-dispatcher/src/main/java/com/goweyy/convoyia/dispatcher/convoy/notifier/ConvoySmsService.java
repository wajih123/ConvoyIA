package com.goweyy.convoyia.dispatcher.convoy.notifier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ConvoySmsService {

    @Value("${convoyia.notifier.sms-enabled:false}")
    private boolean smsEnabled;

    @Value("${convoyia.notifier.twilio-account-sid:}")
    private String accountSid;

    @Value("${convoyia.notifier.twilio-auth-token:}")
    private String authToken;

    @Value("${convoyia.notifier.twilio-from-number:}")
    private String fromNumber;

    public void sendSms(String toNumber, String message) {
        if (!smsEnabled) {
            log.info("[ConvoySms] SMS disabled. Would send to={}: {}", toNumber, message);
            return;
        }
        // TODO: Twilio.init(accountSid, authToken); Message.creator(new PhoneNumber(toNumber), new PhoneNumber(fromNumber), message).create();
        log.warn("[ConvoySms] TODO: Twilio integration not yet wired. to={} message={}", toNumber, message);
    }
}
