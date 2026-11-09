package com.lifeos.notification.delivery.client;

/** Sends a plain-text e-mail. */
public interface EmailSender {

    /** @throws RuntimeException when the mail server refuses or cannot be reached (the delivery is retried) */
    void send(String to, String subject, String text);
}
