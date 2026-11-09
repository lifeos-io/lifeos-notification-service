package com.lifeos.notification.delivery.client;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Writes push messages to the log instead of sending them, until a push provider is chosen. */
@Component
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public void send(List<String> tokens, String title, String body, String link) {
        // Tokens identify devices: only their number goes to the log.
        log.info("Push to {} device(s): \"{}\" ({})", tokens.size(), title, link);
    }
}
