package com.lifeos.notification.delivery.client;

import java.util.List;

/**
 * Sends a push message to a user's devices. The provider (FCM, APNs...) is chosen together with the mobile stack;
 * until then {@link LoggingPushSender} stands in.
 */
public interface PushSender {

    /**
     * @param tokens the user's device tokens (at least one)
     * @throws RuntimeException when the provider refuses or cannot be reached (the delivery is retried)
     */
    void send(List<String> tokens, String title, String body, String link);
}
