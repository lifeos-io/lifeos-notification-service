package com.lifeos.notification.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lifeos.notification.delivery.service.DeliveryRunService;
import com.lifeos.notification.support.Events;
import com.lifeos.notification.support.FailingEmailSender;
import com.lifeos.notification.support.IntegrationTest;
import com.lifeos.notification.support.RecordingPushSender;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** Sending: push to registered devices, e-mail through real SMTP (Mailpit), retries and giving up. */
class DeliveryIntegrationTest extends IntegrationTest {

    @Autowired
    private DeliveryRunService deliveryRunService;

    @Autowired
    private RecordingPushSender recordingPushSender;

    @Autowired
    private FailingEmailSender failingEmailSender;

    @Value("${lifeos.test.mailpit-api}")
    private String mailpitApi;

    private Events events;

    @BeforeEach
    void setUp() {
        events = new Events(jsonMapper);
        clock.set(Instant.parse("2026-11-15T15:00:00Z"));
    }

    @Test
    void withoutADeviceOrEmailAMessageStaysInTheInbox() {
        UUID user = UUID.randomUUID();
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));

        assertEquals(List.of(), deliveries(user));
    }

    @Test
    void pushGoesToTheUsersDevices() throws Exception {
        UUID user = UUID.randomUUID();
        String token = "fcm-" + user;
        registerDevice(user, token);

        event("trigger", user, "DecisionReviewDue",
                events.decisionReviewDue(UUID.randomUUID(), "K8s deneyimi", LocalDate.parse("2026-11-20")));
        assertEquals(List.of("PUSH PENDING 0"), deliveries(user));
        drain();

        assertEquals(List.of("PUSH SENT 1"), deliveries(user));
        RecordingPushSender.Sent sent = recordingPushSender.sentTo(token).getFirst();
        assertEquals("Kararını gözden geçirme zamanı", sent.title());
        assertTrue(sent.link().startsWith("lifeos://decisions/"));
    }

    @Test
    void emailArrivesThroughSmtp() throws Exception {
        UUID user = UUID.randomUUID();
        String address = user + "@lifeos.test";
        enableEmail(user, address);

        event("trigger", user, "DriftDetected", events.driftDetected(UUID.randomUUID(), "2026-W46", 240, 100));
        drain();

        assertEquals(List.of("EMAIL SENT 1"), deliveries(user));
        JsonNode mail = mailTo(address);
        assertEquals("Bu hafta planın gerisindesin", mail.get("Subject").asString());
    }

    @Test
    void aFailingSendIsRetriedWithGrowingPausesThenGivenUp() throws Exception {
        UUID user = UUID.randomUUID();
        String address = user + "@lifeos.test";
        enableEmail(user, address);
        failingEmailSender.failFor(address, 100);
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));

        Instant now = clock.instant();
        List<Duration> pauses = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30),
                Duration.ofHours(2));
        for (int attempt = 1; attempt <= 4; attempt++) {
            drain();
            assertEquals(List.of("EMAIL PENDING " + attempt), deliveries(user));
            Instant next = nextAttempt(user);
            assertEquals(now.plus(pauses.get(attempt - 1)), next);
            // Not before its time.
            clock.set(next.minusSeconds(1));
            drain();
            assertEquals(List.of("EMAIL PENDING " + attempt), deliveries(user));
            clock.set(next);
            now = next;
        }
        drain();

        assertEquals(List.of("EMAIL FAILED 5"), deliveries(user));
        assertTrue(lastError(user).contains("SMTP is down"), lastError(user));
    }

    @Test
    void aFailureFollowedBySuccessIsSent() throws Exception {
        UUID user = UUID.randomUUID();
        String address = user + "@lifeos.test";
        enableEmail(user, address);
        failingEmailSender.failFor(address, 1);
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));

        drain();
        clock.set(clock.instant().plus(Duration.ofMinutes(1)));
        drain();

        assertEquals(List.of("EMAIL SENT 2"), deliveries(user));
        assertEquals("Haftanı kapatma zamanı", mailTo(address).get("Subject").asString());
    }

    @Test
    void aChannelTurnedOffBeforeSendingIsNotUsed() throws Exception {
        UUID user = UUID.randomUUID();
        String address = user + "@lifeos.test";
        enableEmail(user, address);
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));

        mockMvc.perform(put("/api/notifications/preferences").with(as(user, address))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 1, \"pushEnabled\": true, \"emailEnabled\": false}"))
                .andExpect(status().isOk());
        drain();

        assertEquals(List.of("EMAIL FAILED 1"), deliveries(user));
        assertEquals("e-mail is off", lastError(user));
        assertEquals(0, mailsTo(address).size());
    }

    private void registerDevice(UUID user, String token) throws Exception {
        mockMvc.perform(post("/api/notifications/devices").with(as(user)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"platform\": \"ANDROID\"}"))
                .andExpect(status().isNoContent());
    }

    private void enableEmail(UUID user, String address) throws Exception {
        mockMvc.perform(put("/api/notifications/preferences").with(as(user, address))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 0, \"pushEnabled\": true, \"emailEnabled\": true}"))
                .andExpect(status().isOk());
    }

    /** Sends everything due at the clock's time (other tests' deliveries included). */
    private void drain() {
        while (deliveryRunService.runDue(50) > 0) {
            // next batch
        }
    }

    /** The user's deliveries as "CHANNEL STATUS attempts". */
    private List<String> deliveries(UUID user) {
        return jdbcClient.sql("""
                        SELECT channel || ' ' || status || ' ' || attempts FROM delivery WHERE user_id = :u
                        ORDER BY channel
                        """)
                .param("u", user).query(String.class).list();
    }

    private Instant nextAttempt(UUID user) {
        return jdbcClient.sql("SELECT next_attempt_at FROM delivery WHERE user_id = :u").param("u", user)
                .query(Timestamp.class).single().toInstant();
    }

    private String lastError(UUID user) {
        return jdbcClient.sql("SELECT last_error FROM delivery WHERE user_id = :u").param("u", user)
                .query(String.class).single();
    }

    private JsonNode mailTo(String address) throws Exception {
        List<JsonNode> mails = mailsTo(address);
        assertEquals(1, mails.size(), "mails to " + address);
        return mails.getFirst();
    }

    /** Messages Mailpit caught for the address. */
    private List<JsonNode> mailsTo(String address) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(mailpitApi + "/api/v1/messages?limit=500")).build(),
                HttpResponse.BodyHandlers.ofString());
        return jsonMapper.readTree(response.body()).get("messages").valueStream()
                .filter(message -> message.get("To").valueStream()
                        .anyMatch(to -> address.equals(to.get("Address").asString())))
                .toList();
    }
}
