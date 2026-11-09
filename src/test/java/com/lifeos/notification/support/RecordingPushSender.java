package com.lifeos.notification.support;

import com.lifeos.notification.delivery.client.PushSender;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Records push messages instead of logging them, so tests can see what went to which devices. */
public class RecordingPushSender implements PushSender {

    private final List<Sent> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(List<String> tokens, String title, String body, String link) {
        sent.add(new Sent(List.copyOf(tokens), title, body, link));
    }

    public List<Sent> sentTo(String token) {
        return sent.stream().filter(message -> message.tokens().contains(token)).toList();
    }

    public record Sent(List<String> tokens, String title, String body, String link) {
    }
}
