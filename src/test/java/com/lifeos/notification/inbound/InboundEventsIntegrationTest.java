package com.lifeos.notification.inbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lifeos.notification.notification.entity.NotificationKind;
import com.lifeos.notification.notification.service.NotificationService;
import com.lifeos.notification.support.Events;
import com.lifeos.notification.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Events become inbox messages in the user's language, once each. */
class InboundEventsIntegrationTest extends IntegrationTest {

    @Autowired
    private NotificationService notificationService;

    private Events events;

    @BeforeEach
    void events() {
        events = new Events(jsonMapper);
    }

    @Test
    void everyEventForTheUserBecomesAMessageInTurkishByDefault() {
        UUID user = UUID.randomUUID();
        UUID decision = UUID.randomUUID();
        UUID plan = UUID.randomUUID();
        UUID goal = UUID.randomUUID();

        event("trigger", user, "DecisionReviewDue",
                events.decisionReviewDue(decision, "K8s deneyimi", LocalDate.parse("2026-11-20")));
        event("trigger", user, "ReopenTriggerFired", events.reopenTriggerFired(decision, "K8s projesine atanırsam"));
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(plan, "2026-W46"));
        event("trigger", user, "DriftDetected", events.driftDetected(plan, "2026-W46", 240, 100));
        event("strategy", user, "DecisionAssumptionFalsified",
                events.assumptionFalsified(decision, "K8s deneyimi", "İşe alımcılar sadece LinkedIn okur"));
        event("coach", user, "RecommendationProposed", events.recommendationProposed(goal, "Mülakatlar system design soruyor"));

        List<Map<String, Object>> inbox = inbox(user);
        assertEquals(List.of("PROPOSAL_RECEIVED", "ASSUMPTION_FALSIFIED", "DRIFT_DETECTED", "WEEKLY_REVIEW_DUE",
                "REOPEN_TRIGGER_FIRED", "DECISION_REVIEW_DUE"), inbox.stream().map(n -> n.get("kind")).toList());
        Map<String, Object> review = inbox.getLast();
        assertEquals("Kararını gözden geçirme zamanı", review.get("title"));
        assertTrue(((String) review.get("body")).contains("\"K8s deneyimi\" kararını 20 Kasım 2026 tarihinde"),
                (String) review.get("body"));
        assertEquals("lifeos://decisions/" + decision, review.get("link"));
        assertEquals("2026-W46 haftasında planlanan 240 dakikanın 100 dakikası yapıldı. Planı güncellemek ister misin?",
                inbox.get(2).get("body"));
        assertEquals("lifeos://weekly-plans/2026-W46/review", inbox.get(3).get("link"));
        assertEquals("Mülakatlar system design soruyor", inbox.getFirst().get("body"));
        assertEquals("lifeos://goals/" + goal + "/proposals", inbox.getFirst().get("link"));
    }

    @Test
    void messagesFollowTheLanguageOfTheProfile() {
        UUID user = UUID.randomUUID();
        event("strategy", user, "UserProfileUpdated", events.profile("en-US"));

        event("trigger", user, "DecisionReviewDue",
                events.decisionReviewDue(UUID.randomUUID(), "K8s experience", LocalDate.parse("2026-11-20")));
        event("trigger", user, "DriftDetected", events.driftDetected(UUID.randomUUID(), "2026-W46", 240, 100));

        List<Map<String, Object>> inbox = inbox(user);
        assertEquals("You are behind this week's plan", inbox.getFirst().get("title"));
        assertEquals("Time to review a decision", inbox.getLast().get("title"));
        assertTrue(((String) inbox.getLast().get("body")).contains("on November 20, 2026"),
                (String) inbox.getLast().get("body"));
        // A language without its own texts falls back to Turkish.
        event("strategy", user, "UserProfileUpdated", events.profile("de-DE"));
        event("trigger", user, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));
        assertEquals("Haftanı kapatma zamanı", inbox(user).getFirst().get("title"));
    }

    @Test
    void anEventIsAMessageOnceAndEventsForNobodyAreIgnored() {
        UUID user = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        // The same source event twice (e.g. replayed after the inbox forgot it): one message.
        notificationService.notify(user, eventId, NotificationKind.WEEKLY_REVIEW_DUE, null, null, "2026-W46");
        notificationService.notify(user, eventId, NotificationKind.WEEKLY_REVIEW_DUE, null, null, "2026-W46");
        // A profile update is no message.
        event("strategy", user, "UserProfileUpdated", events.profile("tr-TR"));

        assertEquals(1, inbox(user).size());
    }

    @Test
    void aDeletedUserLeavesNothingBehind() throws Exception {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        for (UUID someone : List.of(user, other)) {
            mockMvc.perform(post("/api/notifications/devices").with(as(someone))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"token\": \"token-" + someone + "\", \"platform\": \"ANDROID\"}"))
                    .andExpect(status().isNoContent());
            event("trigger", someone, "WeeklyReviewDue", events.weeklyReviewDue(UUID.randomUUID(), "2026-W46"));
        }

        event("strategy", user, "UserDeletionRequested", events.userDeletionRequested());
        event("strategy", user, "UserDeletionRequested", events.userDeletionRequested());

        assertEquals(Map.of("recipient", 0L, "device", 0L, "notification", 0L, "delivery", 0L), rows(user));
        assertEquals(Map.of("recipient", 1L, "device", 1L, "notification", 1L, "delivery", 1L), rows(other));
    }

    private List<Map<String, Object>> inbox(UUID user) {
        return jdbcClient.sql("""
                        SELECT kind, title, body, link FROM notification WHERE user_id = :u ORDER BY created_at DESC, id DESC
                        """)
                .param("u", user).query().listOfRows();
    }

    private Map<String, Long> rows(UUID user) {
        return Map.of(
                "recipient", count("SELECT count(*) FROM recipient WHERE user_id = :u", user),
                "device", count("SELECT count(*) FROM device WHERE user_id = :u", user),
                "notification", count("SELECT count(*) FROM notification WHERE user_id = :u", user),
                "delivery", count("SELECT count(*) FROM delivery WHERE user_id = :u", user));
    }

    private long count(String sql, UUID user) {
        return jdbcClient.sql(sql).param("u", user).query(Long.class).single();
    }
}
