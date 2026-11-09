package com.lifeos.notification.support;

import com.lifeos.notification.delivery.client.EmailSender;
import com.lifeos.notification.delivery.client.SmtpEmailSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Sends through the real SMTP sender (to Mailpit), but can be told to fail for an address a number of times. */
public class FailingEmailSender implements EmailSender {

    private final SmtpEmailSender smtpEmailSender;
    private final Map<String, AtomicInteger> failuresLeft = new ConcurrentHashMap<>();

    public FailingEmailSender(SmtpEmailSender smtpEmailSender) {
        this.smtpEmailSender = smtpEmailSender;
    }

    public void failFor(String to, int times) {
        failuresLeft.put(to, new AtomicInteger(times));
    }

    @Override
    public void send(String to, String subject, String text) {
        AtomicInteger failures = failuresLeft.get(to);
        if (failures != null && failures.getAndDecrement() > 0) {
            throw new IllegalStateException("SMTP is down (test)");
        }
        smtpEmailSender.send(to, subject, text);
    }
}
